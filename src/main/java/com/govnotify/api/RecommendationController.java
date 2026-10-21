package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    // GET /api/recommendations/new-arrivals — 6 most recent active jobs
    @GetMapping("/new-arrivals")
    public ResponseEntity<List<JobNotification>> getNewArrivals() {
        List<JobNotification> arrivals = recommendationService.getNewArrivals();
        return ResponseEntity.ok(arrivals);
    }

    // GET /api/recommendations/{userEmail} — Profile-tailored jobs
    @GetMapping("/{userEmail}")
    public ResponseEntity<List<JobNotification>> getRecommendedJobs(@PathVariable String userEmail) {
        List<JobNotification> recommended = recommendationService.getRecommendedJobs(userEmail);
        return ResponseEntity.ok(recommended);
    }
}
