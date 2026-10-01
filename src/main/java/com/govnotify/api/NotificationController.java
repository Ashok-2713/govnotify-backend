package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobNotificationRepository jobRepository;

    /**
     * Resolves the current user using Authentication principal, email parameter,
     * header, or fallback to the primary active user.
     */
    private AppUser resolveUser(
            Authentication authentication,
            String emailParam,
            String headerEmail,
            String authHeader,
            Map<String, ?> body
    ) {
        String targetEmail = null;

        if (emailParam != null && !emailParam.trim().isEmpty()) {
            targetEmail = emailParam.trim().toLowerCase();
        } else if (headerEmail != null && !headerEmail.trim().isEmpty()) {
            targetEmail = headerEmail.trim().toLowerCase();
        } else if (body != null && body.get("email") != null) {
            targetEmail = body.get("email").toString().trim().toLowerCase();
        } else if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            targetEmail = authentication.getName().trim().toLowerCase();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (token.contains("@")) {
                targetEmail = token.toLowerCase();
            }
        }

        if (targetEmail != null) {
            Optional<AppUser> userOpt = userRepository.findByEmail(targetEmail);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }

        // Fallback to default user or first user in DB
        return userRepository.findByEmail("ashok.udhay@govnotify.in")
                .or(() -> userRepository.findAll().stream().findFirst())
                .orElse(null);
    }

    /**
     * GET /api/notifications/unread-count
     * Returns the count of jobs added since user's lastNotificationsReadAt.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            Authentication authentication,
            @RequestParam(name = "email", required = false) String emailParam,
            @RequestHeader(name = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        try {
            AppUser user = resolveUser(authentication, emailParam, headerEmail, authHeader, null);

            long count;
            if (user == null || user.getLastNotificationsReadAt() == null) {
                // User has never read notifications or no user tracked -> count all active jobs
                count = jobRepository.countAllActiveJobs();
            } else {
                count = jobRepository.countActiveByCreatedAtAfter(user.getLastNotificationsReadAt());
            }

            return ResponseEntity.ok(Map.of(
                    "count", count,
                    "lastReadAt", user != null && user.getLastNotificationsReadAt() != null
                            ? user.getLastNotificationsReadAt().toString() : "NEVER"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/notifications/mark-as-read
     * Sets user's lastNotificationsReadAt to now, resetting the unread count.
     */
    @PostMapping("/mark-as-read")
    public ResponseEntity<?> markAsRead(
            Authentication authentication,
            @RequestParam(name = "email", required = false) String emailParam,
            @RequestHeader(name = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        try {
            AppUser user = resolveUser(authentication, emailParam, headerEmail, authHeader, body);

            if (user != null) {
                user.setLastNotificationsReadAt(LocalDateTime.now());
                userRepository.save(user);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "markedAt", user.getLastNotificationsReadAt().toString(),
                        "userEmail", user.getEmail()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "markedAt", LocalDateTime.now().toString(),
                        "note", "No user found; default marked."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/notifications/unread-jobs
     * Optional helper endpoint to fetch the actual jobs created since last read.
     */
    @GetMapping("/unread-jobs")
    public ResponseEntity<?> getUnreadJobs(
            Authentication authentication,
            @RequestParam(name = "email", required = false) String emailParam,
            @RequestHeader(name = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        try {
            AppUser user = resolveUser(authentication, emailParam, headerEmail, authHeader, null);

            List<JobNotification> unreadJobs;
            if (user == null || user.getLastNotificationsReadAt() == null) {
                unreadJobs = jobRepository.findAll();
            } else {
                unreadJobs = jobRepository.findJobsCreatedAfter(user.getLastNotificationsReadAt());
            }

            return ResponseEntity.ok(unreadJobs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}