package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing aggregated statistical metrics and platform analytics
 * for the GovNotify Admin Analytics Dashboard.
 */
@Service
public class AnalyticsService {

    @Autowired
    private JobNotificationRepository jobRepository;

    @Autowired
    private UserProfileRepository userRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    /**
     * Retrieves high-level KPI metrics including total jobs, users, saved jobs, and active jobs.
     *
     * @return Map containing overview key-value metrics and generation timestamp.
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalJobs", jobRepository.count());
        overview.put("totalUsers", userRepository.count());
        overview.put("totalSavedJobs", savedJobRepository.count());

        List<JobNotification> openJobs = jobRepository.findByStatus("OPEN");
        int activeCount = (openJobs != null) ? openJobs.size() : 0;
        if (activeCount == 0) {
            List<JobNotification> titleCaseOpen = jobRepository.findByStatus("Open");
            if (titleCaseOpen != null && !titleCaseOpen.isEmpty()) {
                activeCount = titleCaseOpen.size();
            }
        }
        overview.put("activeJobs", activeCount);
        overview.put("generatedAt", LocalDateTime.now().toString());

        return overview;
    }

    /**
     * Aggregates job notifications grouped by state, ordered by volume descending.
     *
     * @return Map of state name to job count.
     */
    public Map<String, Long> getJobsByState() {
        List<JobNotification> allJobs = jobRepository.findAll();
        if (allJobs == null) return Collections.emptyMap();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> {
                            String state = job != null ? job.getState() : null;
                            return (state != null && !state.trim().isEmpty()) ? state.trim() : "Unknown";
                        },
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Aggregates job notifications grouped by job category, ordered by volume descending.
     *
     * @return Map of category name to job count.
     */
    public Map<String, Long> getJobsByCategory() {
        List<JobNotification> allJobs = jobRepository.findAll();
        if (allJobs == null) return Collections.emptyMap();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> {
                            String cat = job != null ? job.getCategory() : null;
                            return (cat != null && !cat.trim().isEmpty()) ? cat.trim() : "Unknown";
                        },
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Aggregates job notifications grouped by lifecycle status, ordered by volume descending.
     *
     * @return Map of status string to job count.
     */
    public Map<String, Long> getJobsByStatus() {
        List<JobNotification> allJobs = jobRepository.findAll();
        if (allJobs == null) return Collections.emptyMap();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> {
                            String status = job != null ? job.getStatus() : null;
                            return (status != null && !status.trim().isEmpty()) ? status.trim() : "Unknown";
                        },
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Identifies the top 10 most bookmarked jobs across the candidate user base.
     *
     * @return List of maps containing jobId, savedCount, and jobTitle for the top 10 saved jobs.
     */
    public List<Map<String, Object>> getTopSavedJobs() {
        List<SavedJob> savedJobs = savedJobRepository.findAll();
        if (savedJobs == null || savedJobs.isEmpty()) {
            return Collections.emptyList();
        }

        // Preload existing jobs for title-to-job ID resolution
        List<JobNotification> existingJobs = jobRepository.findAll();
        Map<String, JobNotification> titleMap = new HashMap<>();
        if (existingJobs != null) {
            for (JobNotification jn : existingJobs) {
                if (jn.getTitle() != null && !jn.getTitle().trim().isEmpty()) {
                    titleMap.putIfAbsent(jn.getTitle().trim().toLowerCase(), jn);
                }
            }
        }

        // Group saved jobs by job title (the identifier stored on SavedJob entity)
        Map<String, Long> countsByTitle = savedJobs.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        sj -> {
                            String title = sj.getJobTitle();
                            return (title != null && !title.trim().isEmpty()) ? title.trim() : "Unknown Job";
                        },
                        Collectors.counting()
                ));

        return countsByTitle.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    String titleKey = entry.getKey();
                    Long count = entry.getValue();

                    JobNotification matchedJob = titleMap.get(titleKey.toLowerCase());
                    Long jobId = (matchedJob != null) ? matchedJob.getId() : null;

                    String resolvedTitle = "Unknown Job";
                    if (jobId != null) {
                        Optional<JobNotification> opt = jobRepository.findById(jobId);
                        if (opt.isPresent() && opt.get().getTitle() != null) {
                            resolvedTitle = opt.get().getTitle();
                        } else {
                            resolvedTitle = titleKey;
                        }
                    } else if (!titleKey.equalsIgnoreCase("Unknown Job")) {
                        resolvedTitle = titleKey;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("jobId", jobId);
                    item.put("savedCount", count);
                    item.put("jobTitle", resolvedTitle);
                    return item;
                })
                .collect(Collectors.toList());
    }
}
