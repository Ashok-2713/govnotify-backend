package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/eligibility")
public class EligibilityController {

    @Autowired
    private JobNotificationRepository jobRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @PostMapping("/check")
    public List<JobNotification> checkEligibility(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String qualification = data.getOrDefault("qualification", "").trim();
        String state = data.getOrDefault("state", "All").trim();
        String category = data.getOrDefault("category", "All").trim();
        int age = 0;
        try { age = Integer.parseInt(data.getOrDefault("age", "0")); } catch (Exception ignored) {}

        if (email != null && !email.isEmpty()) {
            UserProfile profile = userProfileRepository.findByEmail(email).orElse(new UserProfile());
            profile.setEmail(email);
            profile.setQualification(qualification);
            profile.setAge(age);
            profile.setState(state);
            profile.setCategory(category);
            profile.setLastUpdated(LocalDateTime.now());
            userProfileRepository.save(profile);
        }

        return filterJobs(qualification, age, state, category);
    }

    @GetMapping("/get-profile/{email}")
    public UserProfile getProfile(@PathVariable String email) {
        return userProfileRepository.findByEmail(email).orElse(null);
    }

    @DeleteMapping("/clear-profile/{email}")
    public String clearProfile(@PathVariable String email) {
        userProfileRepository.deleteByEmail(email);
        return "Profile cleared";
    }

    private List<JobNotification> filterJobs(String qualification, int age, String state, String category) {
        int userQualLevel = getQualificationLevel(qualification);
        List<JobNotification> allJobs = jobRepository.findAll();
        List<JobNotification> eligible = new ArrayList<>();

        for (JobNotification job : allJobs) {
            if (job.getStatus() != null && job.getStatus().equalsIgnoreCase("CLOSED")) continue;

            String jobQualText = ((job.getQualification() != null ? job.getQualification() : "") + " " +
                                  (job.getTitle() != null ? job.getTitle() : "")).toLowerCase();
            int jobQualLevel = getQualificationLevel(jobQualText);

            if (jobQualLevel > 0 && userQualLevel > 0 && jobQualLevel > userQualLevel) continue;

            if (!state.equalsIgnoreCase("All")) {
                String jobState = (job.getState() != null ? job.getState() : "").toLowerCase();
                boolean stateOk = jobState.contains(state.toLowerCase())
                        || jobState.contains("all india")
                        || jobState.contains("central")
                        || jobState.isEmpty();
                if (!stateOk) continue;
            }

            if (!category.equalsIgnoreCase("All")) {
                String jobCat = (job.getCategory() != null ? job.getCategory() : "").toLowerCase();
                if (!jobCat.isEmpty()
                        && !jobCat.contains("government")
                        && !jobCat.contains(category.toLowerCase())) continue;
            }

            if (age > 0) {
                String ageLimit = (job.getAgeLimit() != null ? job.getAgeLimit() : "").toLowerCase();
                if (ageLimit.contains("-")) {
                    try {
                        String[] parts = ageLimit.replaceAll("[^0-9\\-]", "").split("-");
                        int minAge = Integer.parseInt(parts[0]);
                        int maxAge = Integer.parseInt(parts[1]);
                        if (age < minAge || age > maxAge) continue;
                    } catch (Exception ignored) {}
                }
            }

            eligible.add(job);
        }
        return eligible;
    }

    private int getQualificationLevel(String text) {
        if (text == null || text.isEmpty()) return 0;
        String t = text.toLowerCase();
        if (t.contains("phd") || t.contains("doctorate")) return 7;
        if (t.contains("pg") || t.contains("post graduate") || t.contains("master")
                || t.contains("m.sc") || t.contains("m.com") || t.contains("m.a")
                || t.contains("mba") || t.contains("m.tech")) return 6;
        if (t.contains("b.e") || t.contains("b.tech") || t.contains("engineering")
                || t.contains("btech")) return 5;
        if (t.contains("degree") || t.contains("graduate") || t.contains("bachelor")
                || t.contains("b.sc") || t.contains("b.com") || t.contains("b.a")) return 4;
        if (t.contains("diploma") || t.contains("iti") || t.contains("polytechnic")) return 3;
        if (t.contains("12th") || t.contains("puc") || t.contains("hsc")
                || t.contains("intermediate") || t.contains("10+2")) return 2;
        if (t.contains("10th") || t.contains("sslc") || t.contains("matriculation")) return 1;
        return 0;
    }
}