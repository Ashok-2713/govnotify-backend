package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private JobNotificationRepository jobNotificationRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    /**
     * Checks whether a job notification is currently active.
     */
    private boolean isActive(JobNotification job) {
        if (job == null) return false;
        String status = job.getStatus();
        if (status != null && status.equalsIgnoreCase("Closed")) {
            return false;
        }
        if (job.getLastDate() != null && job.getLastDate().isBefore(LocalDate.now())) {
            return false;
        }
        return true;
    }

    /**
     * Finds jobs tailored to the user's state or qualification, excluding already-saved jobs.
     * If no profile exists, returns the 6 most recently created active jobs.
     */
    public List<JobNotification> getRecommendedJobs(String userEmail) {
        // 1. Fetch user profile if email provided
        Optional<UserProfile> profileOpt = (userEmail != null && !userEmail.trim().isEmpty())
                ? userProfileRepository.findByEmail(userEmail.trim())
                : Optional.empty();

        // 2. Fetch saved jobs to exclude
        Set<String> savedTitles = new HashSet<>();
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            List<SavedJob> savedJobs = savedJobRepository.findByUserEmail(userEmail.trim());
            for (SavedJob sj : savedJobs) {
                if (sj.getJobTitle() != null) {
                    savedTitles.add(sj.getJobTitle().trim().toLowerCase());
                }
            }
        }

        // 3. Fetch active jobs from repository
        List<JobNotification> allJobs = jobNotificationRepository.findAll();
        List<JobNotification> activeJobs = allJobs.stream()
                .filter(this::isActive)
                .filter(j -> j.getTitle() != null && !savedTitles.contains(j.getTitle().trim().toLowerCase()))
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .collect(Collectors.toList());

        // 4. If no profile exists or profile has neither state nor qualification, return top 6 active
        if (profileOpt.isEmpty()) {
            return activeJobs.stream().limit(6).collect(Collectors.toList());
        }

        UserProfile profile = profileOpt.get();
        String userState = profile.getState() != null ? profile.getState().trim().toLowerCase() : "";
        String userQual = profile.getQualification() != null ? profile.getQualification().trim().toLowerCase() : "";

        if (userState.isEmpty() && userQual.isEmpty()) {
            return activeJobs.stream().limit(6).collect(Collectors.toList());
        }

        // 5. Filter matching user's state OR qualification
        List<JobNotification> matchedJobs = new ArrayList<>();
        List<JobNotification> otherJobs = new ArrayList<>();

        for (JobNotification job : activeJobs) {
            String jobState = job.getState() != null ? job.getState().trim().toLowerCase() : "";
            String jobQual = job.getQualification() != null ? job.getQualification().trim().toLowerCase() : "";

            boolean stateMatch = !userState.isEmpty() && !jobState.isEmpty() &&
                    (jobState.contains(userState) || userState.contains(jobState) || jobState.equals("all-india"));
            boolean qualMatch = !userQual.isEmpty() && !jobQual.isEmpty() &&
                    (jobQual.contains(userQual) || userQual.contains(jobQual));

            if (stateMatch || qualMatch) {
                matchedJobs.add(job);
            } else {
                otherJobs.add(job);
            }
        }

        // Top 6 matching jobs; if fewer than 6, fill with other recent active jobs
        List<JobNotification> result = new ArrayList<>(matchedJobs);
        if (result.size() < 6) {
            for (JobNotification other : otherJobs) {
                if (result.size() >= 6) break;
                result.add(other);
            }
        }

        return result.stream().limit(6).collect(Collectors.toList());
    }

    /**
     * Returns the 6 most recently created active jobs (status OPEN or UPCOMING).
     */
    public List<JobNotification> getNewArrivals() {
        return jobNotificationRepository.findAll().stream()
                .filter(this::isActive)
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .limit(6)
                .collect(Collectors.toList());
    }
}
