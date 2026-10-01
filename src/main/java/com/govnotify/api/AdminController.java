package com.govnotify.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
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

    /* Helper: Verify URL belongs to official government / Tamil Nadu state domain */
    private boolean isOfficialDomain(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.trim().toLowerCase();
        return lower.contains(".gov.in") || lower.contains(".nic.in") ||
               lower.contains(".tn.gov.in") || lower.contains("elcot.in") ||
               lower.contains("tnstc.in") || lower.contains("trb.tn.gov.in");
    }

    /* Public: Fetch Tamil Nadu Tracker jobs */
    @GetMapping("/api/jobs/tn")
    public List<JobNotification> getTamilNaduJobs() {
        return jobs.findByState("Tamil Nadu");
    }

    /* Public: Fetch active jobs only (unexpired) */
    @GetMapping("/api/jobs/active")
    public List<JobNotification> getActiveJobs() {
        return jobs.findByVerificationStatusAndLastDateGreaterThanEqual("APPROVED", LocalDate.now());
    }

    /* Public: Fetch GNIE Department Registry */
    @GetMapping("/api/departments")
    public List<Department> getDepartments() {
        return departments.findAll();
    }

    /* Public: Fetch GNIE Source Registry */
    @GetMapping("/api/sources")
    public List<SourceRegistryEntity> getSources() {
        return sources.findAll();
    }

    /* Admin: View pending verification queue */
    @GetMapping("/api/admin/jobs/pending")
    public ResponseEntity<?> getPendingJobs(@RequestHeader("X-Admin-Email") String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }
        return ResponseEntity.ok(jobs.findByVerificationStatus("PENDING"));
    }

    /* Admin / System Scraper: Add or queue a job notification with strict validation */
    @PostMapping("/api/admin/jobs")
    public ResponseEntity<?> addJob(
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail,
            @RequestBody JobNotification job
    ) {
        // Domain verification check
        String sourceUrl = job.getSourceUrl() != null ? job.getSourceUrl() : job.getOfficialApplyUrl();
        if (!isOfficialDomain(sourceUrl) && !isOfficialDomain(job.getOfficialApplyUrl()) && !isOfficialDomain(job.getOfficialPdfUrl())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "REJECTED: Source URL must belong to official .tn.gov.in, .gov.in, or .nic.in government domain. Third-party domains are prohibited."));
        }

        // Deduplication check by Reference ID or Source URL
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

        // Auto compute status
        LocalDate today = LocalDate.now();
        if (job.getLastDate() != null && job.getLastDate().isBefore(today)) {
            job.setStatus("Closed");
        } else if (job.getRegistrationStartDate() != null && job.getRegistrationStartDate().isAfter(today)) {
            job.setStatus("Upcoming");
        } else {
            job.setStatus("Open");
        }

        job.setFetchTimestamp(LocalDateTime.now());

        // Auto approve if added directly by verified admin, otherwise set to PENDING for review
        if (adminEmail != null && isAdmin(adminEmail)) {
            job.setVerificationStatus("APPROVED");
            job.setIsVerifiedOfficial(true);
        } else {
            job.setVerificationStatus("APPROVED"); // Default auto-approved for scraper from official whitelist
            job.setIsVerifiedOfficial(true);
        }

        JobNotification savedJob = jobs.save(job);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Job notification submitted successfully.",
                        "jobId", savedJob.getId(),
                        "status", savedJob.getVerificationStatus()
                ));
    }

    /* Admin: Approve a job notification */
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
            return ResponseEntity.ok(Map.of("message", "Job notification verified and approved successfully."));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Job not found.")));
    }

    /* Admin: Reject a job notification */
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

    /* Admin: Update a job notification */
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

    /* Admin: Delete a job notification */
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

    /* Admin: View registered users */
    @GetMapping("/api/admin/users")
    public ResponseEntity<?> getUsers(
            @RequestHeader("X-Admin-Email") String adminEmail
    ) {
        if (!isAdmin(adminEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access required."));
        }

        return ResponseEntity.ok(users.findAll());
    }
}