package com.govnotify.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://govnotify-frontend.vercel.app",
        "*"
})
public class AdminController {

    private final JobNotificationRepository jobs;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final SourceRegistryRepository sources;

    public AdminController(
            JobNotificationRepository jobs,
            UserRepository users,
            DepartmentRepository departments,
            SourceRegistryRepository sources
    ) {
        this.jobs = jobs;
        this.users = users;
        this.departments = departments;
        this.sources = sources;
    }

    private boolean isAdmin(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return users.findByEmail(email.trim().toLowerCase())
                .map(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
                .orElse(false);
    }

    private boolean isOfficialDomain(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.trim().toLowerCase();
        return lower.contains(".gov.in") || lower.contains(".nic.in") ||
               lower.contains(".tn.gov.in") || lower.contains("elcot.in") ||
               lower.contains("tnstc.in") || lower.contains("trb.tn.gov.in");
    }

    @GetMapping("/api/jobs/tn")
    public List<JobNotification> getTamilNaduJobs() {
        return jobs.findByState("Tamil Nadu");
    }

    @GetMapping("/api/jobs/active")
    public List<JobNotification> getActiveJobs() {
        return jobs.findByVerificationStatusAndLastDateGreaterThanEqual("APPROVED", LocalDate.now());
    }

    @GetMapping("/api/departments")
    public List<Department> getDepartments() {
        return departments.findAll();
    }

    @GetMapping("/api/sources")
    public List<SourceRegistryEntity> getSources() {
        return sources.findAll();
    }

    /* Admin: View pending verification queue */
    @GetMapping("/api/admin/jobs/pending")
    public ResponseEntity<?> getPendingJobs(
            @RequestHeader(value = "X-Admin-Email", required = false) String headerEmail,
            @RequestParam(value = "email", required = false) String paramEmail
    ) {
        String adminEmail = headerEmail != null ? headerEmail : paramEmail;
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }
        return ResponseEntity.ok(jobs.findByVerificationStatus("PENDING"));
    }

    /* Admin: Add or queue a job notification */
    @PostMapping("/api/admin/jobs")
    public ResponseEntity<?> addJob(
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail,
            @RequestBody JobNotification job
    ) {
        String sourceUrl = job.getSourceUrl() != null ? job.getSourceUrl() : job.getOfficialApplyUrl();
        if (!isOfficialDomain(sourceUrl) && !isOfficialDomain(job.getOfficialApplyUrl()) && !isOfficialDomain(job.getOfficialPdfUrl())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "REJECTED: Source URL must belong to official .tn.gov.in, .gov.in, or .nic.in government domain."));
        }

        if (job.getNotificationReferenceId() != null && !job.getNotificationReferenceId().isBlank()) {
            if (jobs.existsByNotificationReferenceId(job.getNotificationReferenceId().trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "DUPLICATE: Notification reference ID already exists."));
            }
        }
        if (job.getSourceUrl() != null && !job.getSourceUrl().isBlank()) {
            if (jobs.existsBySourceUrl(job.getSourceUrl().trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "DUPLICATE: Source URL already imported into database."));
            }
        }

        LocalDate today = LocalDate.now();
        if (job.getLastDate() != null && job.getLastDate().isBefore(today)) {
            job.setStatus("Closed");
        } else if (job.getRegistrationStartDate() != null && job.getRegistrationStartDate().isAfter(today)) {
            job.setStatus("Upcoming");
        } else {
            job.setStatus("Open");
        }

        job.setFetchTimestamp(LocalDateTime.now());
        job.setVerificationStatus("APPROVED");
        job.setIsVerifiedOfficial(true);

        JobNotification savedJob = jobs.save(job);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Job notification submitted successfully.",
                        "jobId", savedJob.getId(),
                        "status", savedJob.getVerificationStatus()
                ));
    }

    @PutMapping("/api/admin/jobs/{id}/approve")
    public ResponseEntity<?> approveJob(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id
    ) {
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }
        return jobs.findById(id).map(job -> {
            job.setVerificationStatus("APPROVED");
            job.setIsVerifiedOfficial(true);
            jobs.save(job);
            return ResponseEntity.ok(Map.of("message", "Job notification approved successfully."));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Job not found.")));
    }

    @PutMapping("/api/admin/jobs/{id}/reject")
    public ResponseEntity<?> rejectJob(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id
    ) {
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }
        return jobs.findById(id).map(job -> {
            job.setVerificationStatus("REJECTED");
            jobs.save(job);
            return ResponseEntity.ok(Map.of("message", "Job notification rejected."));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Job not found.")));
    }

    @PutMapping("/api/admin/jobs/{id}")
    public ResponseEntity<?> updateJob(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id,
            @RequestBody JobNotification jobDetails
    ) {
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }

        return jobs.findById(id)
                .map(job -> {
                    job.setTitle(jobDetails.getTitle());
                    job.setOrganization(jobDetails.getOrganization());
                    job.setSector(jobDetails.getSector());
                    job.setExamName(jobDetails.getExamName());
                    job.setCategory(jobDetails.getCategory());
                    job.setState(jobDetails.getState());
                    job.setLocation(jobDetails.getLocation());
                    job.setQualification(jobDetails.getQualification());
                    job.setAgeLimit(jobDetails.getAgeLimit());
                    job.setVacancies(jobDetails.getVacancies());
                    job.setRegistrationStartDate(jobDetails.getRegistrationStartDate());
                    job.setLastDate(jobDetails.getLastDate());
                    job.setExamDate(jobDetails.getExamDate());
                    job.setSalary(jobDetails.getSalary());
                    job.setOfficialApplyUrl(jobDetails.getOfficialApplyUrl());
                    job.setOfficialPdfUrl(jobDetails.getOfficialPdfUrl());
                    job.setSourceUrl(jobDetails.getSourceUrl());
                    job.setSourceWebsiteName(jobDetails.getSourceWebsiteName());
                    job.setDescription(jobDetails.getDescription());
                    if (jobDetails.getStatus() != null) job.setStatus(jobDetails.getStatus());

                    jobs.save(job);

                    return ResponseEntity.ok(
                            Map.of("message", "Job notification updated successfully.")
                    );
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Job notification not found.")));
    }

    @DeleteMapping("/api/admin/jobs/{id}")
    public ResponseEntity<?> deleteJob(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id
    ) {
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }
        if (!jobs.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job notification not found."));
        }
        jobs.deleteById(id);
        return ResponseEntity.ok(
                Map.of("message", "Job notification deleted successfully.")
        );
    }

    /* Admin: View registered users (accepts header OR query param) */
    @GetMapping("/api/admin/users")
    public ResponseEntity<?> getUsers(
            @RequestHeader(value = "X-Admin-Email", required = false) String headerEmail,
            @RequestParam(value = "email", required = false) String paramEmail
    ) {
        String adminEmail = headerEmail != null ? headerEmail : paramEmail;

        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }

        List<AppUser> allUsers = users.findAll();
        List<Map<String, Object>> userList = allUsers.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("state", u.getState());
            map.put("district", u.getDistrict());
            map.put("role", u.getRole() != null ? u.getRole() : "USER");
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }
}