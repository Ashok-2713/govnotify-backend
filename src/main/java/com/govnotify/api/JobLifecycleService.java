package com.govnotify.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class JobLifecycleService {

    @Autowired
    private JobNotificationRepository jobRepository;

    private static final Logger log = LoggerFactory.getLogger(JobLifecycleService.class);

    @Scheduled(cron = "0 0 0 * * *")  // Runs daily at midnight
    @Transactional
    public void updateJobLifecycles() {
        LocalDate today = LocalDate.now();
        int opened = jobRepository.markAsOpen(today);
        int closingSoon = jobRepository.markAsClosingSoon(today, today.plusDays(3));
        int closed = jobRepository.markAsClosed(today);
        int archived = jobRepository.markAsArchived(today.minusDays(30));
        log.info("Job lifecycle update: {} opened, {} closing soon, {} closed, {} archived", 
                 opened, closingSoon, closed, archived);
    }
}
