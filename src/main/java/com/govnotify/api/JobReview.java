package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_reviews")
public class JobReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Integer rating; // 1–5

    @Column(length = 2000)
    private String reviewText;

    private LocalDateTime createdAt;

    public JobReview() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
