package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_sessions")
public class GuestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String guestId;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    public GuestSession() {
    }

    public GuestSession(String guestId, LocalDateTime loginTime) {
        this.guestId = guestId;
        this.loginTime = loginTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
