package com.govnotify.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobNotificationRepository extends JpaRepository<JobNotification, Long> {

    List<JobNotification> findByVerificationStatus(String verificationStatus);

    List<JobNotification> findByState(String state);

    List<JobNotification> findByStateIgnoreCase(String state);

    List<JobNotification> findByCentralCategory(String centralCategory);

    List<JobNotification> findByCentralCategoryIgnoreCase(String centralCategory);

    List<JobNotification> findBySector(String sector);

    List<JobNotification> findByStatus(String status);

    List<JobNotification> findByStatusIn(List<String> statuses);

    List<JobNotification> findByVerificationStatusAndLastDateGreaterThanEqual(String verificationStatus, LocalDate date);

    Optional<JobNotification> findByNotificationReferenceId(String notificationReferenceId);

    Optional<JobNotification> findBySourceUrl(String sourceUrl);

    boolean existsByNotificationReferenceId(String notificationReferenceId);

    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT COUNT(j) FROM JobNotification j WHERE j.createdAt > :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(j) FROM JobNotification j")
    long countAllJobs();

    @Query("SELECT COUNT(j) FROM JobNotification j WHERE j.createdAt > :since AND j.status IN ('OPEN', 'UPCOMING', 'CLOSING SOON')")
    long countActiveByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(j) FROM JobNotification j WHERE j.status IN ('OPEN', 'UPCOMING', 'CLOSING SOON')")
    long countAllActiveJobs();

    @Query("SELECT j FROM JobNotification j WHERE j.createdAt > :since ORDER BY j.createdAt DESC")
    List<JobNotification> findJobsCreatedAfter(@Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE JobNotification j SET j.status = 'OPEN', j.statusUpdatedAt = CURRENT_TIMESTAMP WHERE j.status = 'UPCOMING' AND j.registrationStartDate <= :today")
    int markAsOpen(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE JobNotification j SET j.status = 'CLOSING SOON', j.statusUpdatedAt = CURRENT_TIMESTAMP WHERE j.status = 'OPEN' AND j.lastDate BETWEEN :today AND :threeDays")
    int markAsClosingSoon(@Param("today") LocalDate today, @Param("threeDays") LocalDate threeDays);

    @Modifying
    @Query("UPDATE JobNotification j SET j.status = 'CLOSED', j.closedReason = 'Deadline passed', j.statusUpdatedAt = CURRENT_TIMESTAMP WHERE j.status IN ('OPEN', 'CLOSING SOON') AND j.lastDate < :today")
    int markAsClosed(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE JobNotification j SET j.status = 'ARCHIVED', j.archivedAt = CURRENT_TIMESTAMP WHERE j.status = 'CLOSED' AND j.lastDate < :thirtyDaysAgo")
    int markAsArchived(@Param("thirtyDaysAgo") LocalDate thirtyDaysAgo);
}
