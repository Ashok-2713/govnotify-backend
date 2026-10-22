package com.govnotify.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/guest")
public class GuestController {

    @Autowired
    private GuestSessionRepository guestSessionRepository;

    @PostMapping("/login")
    public ResponseEntity<?> guestLogin(@RequestBody Map<String, String> payload) {
        String guestId = payload.get("guestId");
        if (guestId == null || guestId.trim().isEmpty()) {
            guestId = "guest_" + System.currentTimeMillis();
        }

        GuestSession session = new GuestSession();
        session.setGuestId(guestId.trim());
        session.setLoginTime(LocalDateTime.now());
        GuestSession saved = guestSessionRepository.save(session);

        return ResponseEntity.ok(Map.of(
            "message", "Guest session initialized successfully.",
            "guestId", saved.getGuestId(),
            "loginTime", saved.getLoginTime().toString()
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<List<GuestSession>> getAllGuestSessions() {
        List<GuestSession> sessions = guestSessionRepository.findAllByOrderByLoginTimeDesc();
        return ResponseEntity.ok(sessions);
    }
}
