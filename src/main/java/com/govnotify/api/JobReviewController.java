package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reviews")
public class JobReviewController {

    @Autowired
    private JobReviewRepository jobReviewRepository;

    // POST /api/reviews — Submit a new review
    @PostMapping
    public ResponseEntity<?> submitReview(@RequestBody JobReview review) {
        if (review.getUserEmail() == null || review.getUserEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5."));
        }
        review.setCreatedAt(LocalDateTime.now());
        JobReview saved = jobReviewRepository.save(review);
        return ResponseEntity.ok(saved);
    }

    // GET /api/reviews/{jobId} — Fetch reviews for a job
    @GetMapping("/{jobId}")
    public ResponseEntity<List<JobReview>> getReviews(@PathVariable Long jobId) {
        List<JobReview> reviews = jobReviewRepository.findByJobIdOrderByCreatedAtDesc(jobId);
        return ResponseEntity.ok(reviews);
    }
}
