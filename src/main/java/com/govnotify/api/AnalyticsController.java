package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing endpoints for the GovNotify Admin Analytics Dashboard.
 * Provides aggregated analytics on jobs, user counts, application states, and bookmark statistics.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Endpoint returning general KPI overview stats including total jobs, users, saved jobs, and active jobs.
     *
     * @return ResponseEntity with the overview map or 500 on server error.
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> data = analyticsService.getOverview();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint returning job counts aggregated by state.
     *
     * @return ResponseEntity with state-count map or 500 on server error.
     */
    @GetMapping("/jobs-by-state")
    public ResponseEntity<Map<String, Long>> getJobsByState() {
        try {
            Map<String, Long> data = analyticsService.getJobsByState();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint returning job counts aggregated by category.
     *
     * @return ResponseEntity with category-count map or 500 on server error.
     */
    @GetMapping("/jobs-by-category")
    public ResponseEntity<Map<String, Long>> getJobsByCategory() {
        try {
            Map<String, Long> data = analyticsService.getJobsByCategory();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint returning job counts aggregated by status (e.g. Open, Closed, Archived).
     *
     * @return ResponseEntity with status-count map or 500 on server error.
     */
    @GetMapping("/jobs-by-status")
    public ResponseEntity<Map<String, Long>> getJobsByStatus() {
        try {
            Map<String, Long> data = analyticsService.getJobsByStatus();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint returning the top 10 most bookmarked jobs across candidates.
     *
     * @return ResponseEntity with list of top saved jobs or 500 on server error.
     */
    @GetMapping("/top-saved-jobs")
    public ResponseEntity<List<Map<String, Object>>> getTopSavedJobs() {
        try {
            List<Map<String, Object>> data = analyticsService.getTopSavedJobs();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
