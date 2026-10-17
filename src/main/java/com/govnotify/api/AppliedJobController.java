package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/applied-jobs")
public class AppliedJobController {

    @Autowired
    private AppliedJobRepository appliedJobRepository;

    // ── Existing: Mark applied (used by AppliedJobs.jsx Tab 2 "Mark as Applied") ──
    @PostMapping("/mark")
    public String markApplied(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String jobTitle = payload.get("jobTitle");
            String organization = payload.get("organization");
            String lastDate = payload.get("lastDate");
            String officialApplyUrl = payload.get("officialApplyUrl");

            if (email == null || email.trim().isEmpty()) {
                return "Error: Email is required.";
            }

            if (appliedJobRepository.existsByUserEmailAndJobTitle(email, jobTitle)) {
                return "Already marked as applied.";
            }

            AppliedJob job = new AppliedJob();
            job.setUserEmail(email);
            job.setJobTitle(jobTitle);
            job.setOrganization(organization);
            job.setLastDate(lastDate);
            job.setOfficialApplyUrl(officialApplyUrl);
            job.setAppliedAt(LocalDateTime.now());
            job.setStatus("APPLIED");
            appliedJobRepository.save(job);

            return "Marked as applied.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ── New: Apply via UserDashboard "Apply" button (uses userEmail + jobId) ──
    @PostMapping("/apply")
    public ResponseEntity<?> applyJob(@RequestBody Map<String, String> payload) {
        try {
            String userEmail = payload.get("userEmail");
            String jobTitle = payload.get("jobTitle");
            String organization = payload.get("organization");
            String lastDate = payload.get("lastDate");
            String officialApplyUrl = payload.get("officialApplyUrl");
            String jobIdStr = payload.get("jobId");

            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Error: Email is required."));
            }

            if (appliedJobRepository.existsByUserEmailAndJobTitle(userEmail, jobTitle)) {
                return ResponseEntity.ok(Map.of("message", "Already in your Application Tracker."));
            }

            AppliedJob job = new AppliedJob();
            job.setUserEmail(userEmail);
            job.setJobTitle(jobTitle);
            job.setOrganization(organization);
            job.setLastDate(lastDate);
            job.setOfficialApplyUrl(officialApplyUrl);
            job.setAppliedAt(LocalDateTime.now());
            job.setStatus("APPLIED");

            if (jobIdStr != null && !jobIdStr.isBlank()) {
                try {
                    job.setJobId(Long.parseLong(jobIdStr));
                } catch (NumberFormatException ignored) {}
            }

            appliedJobRepository.save(job);
            return ResponseEntity.ok(Map.of("message", "Job added to Application Tracker!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Existing: List applied jobs for a user ──
    @GetMapping("/list/{email}")
    public List<AppliedJob> getAppliedJobs(@PathVariable String email) {
        return appliedJobRepository.findByUserEmail(email);
    }

    // ── Existing: Remove a job from applied list ──
    @DeleteMapping("/unmark/{id}")
    public String unmarkJob(@PathVariable Long id) {
        try {
            appliedJobRepository.deleteById(id);
            return "Removed from applied jobs.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ── New: Update status of an applied job ──
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return appliedJobRepository.findById(id).map(job -> {
            job.setStatus(payload.get("status"));
            appliedJobRepository.save(job);
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
