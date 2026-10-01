package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
public class JobController {

    @Autowired
    private JobNotificationRepository jobRepository;

    @Autowired
    private SavedJobController savedJobController;

    @Autowired
    private JobLifecycleService jobLifecycleService;

    /**
     * Admin/manual trigger to update lifecycle statuses on demand.
     */
    @PostMapping("/api/admin/jobs/update-lifecycle")
    public ResponseEntity<?> triggerLifecycle() {
        jobLifecycleService.updateJobLifecycles();
        return ResponseEntity.ok(Map.of("success", true, "message", "Lifecycle updated"));
    }

    /**
     * Supports fetching jobs filtered by scope, state, and/or centralCategory.
     * Scope options:
     *   - active (default): OPEN, UPCOMING, CLOSING SOON
     *   - closed: CLOSED
     *   - archived: ARCHIVED
     *   - all: returns all jobs regardless of status
     * Maps to /api/jobs, /api/jobs/all, and /jobs for compatibility.
     */
    @GetMapping({"/api/jobs", "/api/jobs/all", "/jobs"})
    public ResponseEntity<List<JobNotification>> getJobs(
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String centralCategory,
        @RequestParam(defaultValue = "active") String scope
    ) {
        try {
            List<JobNotification> jobs;
            String normalizedScope = (scope != null) ? scope.trim().toLowerCase() : "active";

            switch (normalizedScope) {
                case "active":
                    jobs = jobRepository.findByStatusIn(List.of("OPEN", "UPCOMING", "CLOSING SOON"));
                    break;
                case "closed":
                    jobs = jobRepository.findByStatus("CLOSED");
                    break;
                case "archived":
                    jobs = jobRepository.findByStatus("ARCHIVED");
                    break;
                default:
                    jobs = jobRepository.findAll();
                    break;
            }

            if (state != null && !state.trim().isEmpty() && !state.equalsIgnoreCase("All")) {
                String trimmedState = state.trim();
                if (trimmedState.equalsIgnoreCase("All India") || trimmedState.equalsIgnoreCase("All Central")) {
                    jobs = jobs.stream()
                            .filter(j -> "All India".equalsIgnoreCase(j.getState()))
                            .collect(Collectors.toList());
                } else {
                    jobs = jobs.stream()
                            .filter(j -> trimmedState.equalsIgnoreCase(j.getState()))
                            .collect(Collectors.toList());
                }
            }

            if (centralCategory != null && !centralCategory.trim().isEmpty()) {
                String trimmedCat = centralCategory.trim();
                jobs = jobs.stream()
                        .filter(j -> trimmedCat.equalsIgnoreCase(j.getCentralCategory()))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/api/jobs/save")
    public String saveJob(@RequestBody Map<String, String> payload) {
        return savedJobController.saveJob(payload);
    }
}
