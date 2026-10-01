package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:5173", "*"})
@RestController
@RequestMapping("/api/applied-jobs")
public class AppliedJobController {

    @Autowired
    private AppliedJobRepository appliedJobRepository;

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
            appliedJobRepository.save(job);

            return "Marked as applied.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/list/{email}")
    public List<AppliedJob> getAppliedJobs(@PathVariable String email) {
        return appliedJobRepository.findByUserEmail(email);
    }

    @DeleteMapping("/unmark/{id}")
    public String unmarkJob(@PathVariable Long id) {
        try {
            appliedJobRepository.deleteById(id);
            return "Removed from applied jobs.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
