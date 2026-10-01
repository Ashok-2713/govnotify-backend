package com.govnotify.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
public class JobImportController {

    @Autowired
    private JobNotificationRepository jobRepository;

    private static final String FILE_PATH = "C:/Users/Ashok P/Downloads/govnotify_agent/master_jobs.json";

    private String categorizeCentralJob(String organization, String title) {
        String org = (organization != null ? organization : "").toLowerCase();
        String ttl = (title != null ? title : "").toLowerCase();
        String combined = org + " " + ttl;

        if (combined.contains("railway") || combined.contains("rrb") || combined.contains("rrc") || combined.contains("irctc")) {
            return "Railways";
        }
        if (combined.contains("ssc") || combined.contains("staff selection")) {
            return "SSC";
        }
        if (combined.contains("bank") || combined.contains("ibps") || combined.contains("sbi") || combined.contains("rbi") || combined.contains("nabard")) {
            return "Banking";
        }
        if (combined.contains("india post") || combined.contains("postal") || combined.contains("gds") || combined.contains("post office")) {
            return "India Post";
        }
        if (combined.contains("upsc") || combined.contains("union public service")) {
            return "UPSC";
        }
        return "Other Central";
    }

    @PostMapping("/api/import-jobs")
    public String importJobs() {
        try {
            // Read the file directly
            Path path = Paths.get(FILE_PATH);
            if (!Files.exists(path)) {
                return "File master_jobs.json not found.";
            }

            String jsonContent = Files.readString(path);
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            // Parse the JSON
            List<JobNotification> jobs = mapper.readValue(jsonContent, new com.fasterxml.jackson.core.type.TypeReference<List<JobNotification>>(){});
            
            // ==========================================
            // 1. GET ALREADY EXISTING JOBS FROM DATABASE
            // ==========================================
            List<JobNotification> existingJobs = jobRepository.findAll();
            
            // ==========================================
            // 2. CREATE A SET OF UNIQUE KEYS FROM EXISTING JOBS
            // ==========================================
            // Using "Title + Organization + State" as the unique key
            List<String> existingKeys = new ArrayList<>();
            for (JobNotification job : existingJobs) {
                existingKeys.add((job.getTitle() != null ? job.getTitle() : "") + "|" + 
                                 (job.getOrganization() != null ? job.getOrganization() : "") + "|" + 
                                 (job.getState() != null ? job.getState() : ""));
            }
            
            // ==========================================
            // 3. FIND ONLY THE NEW JOBS (NOT IN DATABASE)
            // ==========================================
            List<JobNotification> newJobs = new ArrayList<>();
            
            for (JobNotification job : jobs) {
                // Create key for the new job
                String jobKey = (job.getTitle() != null ? job.getTitle() : "") + "|" + 
                                (job.getOrganization() != null ? job.getOrganization() : "") + "|" + 
                                (job.getState() != null ? job.getState() : "");
                
                // If this job is NOT already in the database, add it
                if (!existingKeys.contains(jobKey)) {
                    // Normalize State
                    String state = job.getState();
                    if (state == null || state.equalsIgnoreCase("Central") || state.equalsIgnoreCase("National") || state.equalsIgnoreCase("All States") || state.equalsIgnoreCase("Central Government")) {
                        state = "All India";
                        job.setState("All India");
                    }

                    // Auto-categorize Central jobs
                    if ("All India".equalsIgnoreCase(state)) {
                        if (job.getCentralCategory() == null || job.getCentralCategory().trim().isEmpty()) {
                            job.setCentralCategory(categorizeCentralJob(job.getOrganization(), job.getTitle()));
                        }
                    } else {
                        job.setCentralCategory(null);
                    }

                    // Fill in missing fields
                    if (job.getCategory() == null) job.setCategory("Government Job");
                    if (job.getSourceUrl() == null) job.setSourceUrl("https://www.gov.in");
                    if (job.getSourceWebsiteName() == null) job.setSourceWebsiteName("Official Portal");
                    if (job.getStatus() == null) job.setStatus("Open");
                    if (job.getExtractionMethod() == null) job.setExtractionMethod("AI_NLP");
                    if (job.getVerificationStatus() == null) job.setVerificationStatus("APPROVED");
                    if (job.getIsVerifiedOfficial() == null) job.setIsVerifiedOfficial(true);
                    if (job.getSiteStatus() == null) job.setSiteStatus("HEALTHY");
                    
                    newJobs.add(job);
                }
            }
            
            // ==========================================
            // 4. SAVE ONLY THE NEW JOBS
            // ==========================================
            jobRepository.saveAll(newJobs);
            
            return "Successfully added " + newJobs.size() + " new jobs! Existing " + existingJobs.size() + " jobs kept as is.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error importing jobs: " + e.getMessage();
        }
    }
}
