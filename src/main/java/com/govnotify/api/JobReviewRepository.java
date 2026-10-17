package com.govnotify.api;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobReviewRepository extends JpaRepository<JobReview, Long> {
    List<JobReview> findByJobIdOrderByCreatedAtDesc(Long jobId);
}
