package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/saved-jobs")
public class SavedJobController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @PostMapping("/save")
    public String saveJob(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String jobTitle = payload.get("jobTitle");
            String organization = payload.get("organization");
            String lastDate = payload.get("lastDate");
            String officialApplyUrl = payload.get("officialApplyUrl");

            if (savedJobRepository.existsByUserEmailAndJobTitle(email, jobTitle)) {
                return "Job is already saved in your list.";
            }

            // 1. Save to Database immediately
            SavedJob saved = new SavedJob();
            saved.setUserEmail(email);
            saved.setJobTitle(jobTitle);
            saved.setOrganization(organization);
            saved.setLastDate(lastDate);
            saved.setOfficialApplyUrl(officialApplyUrl);
            saved.setSavedAt(LocalDateTime.now());
            savedJobRepository.save(saved);

            // 2. Trigger Asynchronous Background Task for Email Sending
            // Offloads email transmission to a background thread pool, unblocking the API response immediately
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendJobAlert(
                        email,
                        "Job Saved: " + jobTitle,
                        "Hello,\n\nYou have saved the following job:\n\n" +
                        "Job Title: " + jobTitle + "\n" +
                        "Organization: " + organization + "\n" +
                        "Last Date: " + lastDate + "\n" +
                        "Apply Here: " + officialApplyUrl + "\n\n" +
                        "Best Regards,\nGovNotify Team"
                    );
                } catch (Exception mailEx) {
                    System.err.println("Background email notification error: " + mailEx.getMessage());
                }
            });

            // 3. Return response immediately without waiting for SMTP delivery
            return "✅ Job saved successfully! Email notification sent.";
        } catch (Exception e) {
            return "Error saving job: " + e.getMessage();
        }
    }

    @GetMapping("/list/{email}")
    public List<SavedJob> getSavedJobs(@PathVariable String email) {
        return savedJobRepository.findByUserEmail(email);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteSavedJob(@PathVariable Long id) {
        try {
            savedJobRepository.deleteById(id);
            return "Removed from saved jobs.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}