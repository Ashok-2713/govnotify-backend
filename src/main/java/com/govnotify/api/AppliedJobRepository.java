package com.govnotify.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface AppliedJobRepository extends JpaRepository<AppliedJob, Long> {
    List<AppliedJob> findByUserEmail(String userEmail);

    @Transactional
    @Modifying
    void deleteByUserEmailAndId(String userEmail, Long id);

    @Transactional
    @Modifying
    void deleteByUserEmail(String userEmail);

    boolean existsByUserEmailAndJobTitle(String userEmail, String jobTitle);
}
