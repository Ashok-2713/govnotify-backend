package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_devices")
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String deviceName;

    @Column(length = 50)
    private String deviceType; // desktop, mobile, tablet

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 50)
    private String lastActive;

    @Column
    private Boolean currentSession = false;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserDevice() {}

    public UserDevice(String userEmail, String deviceName, String deviceType, String location, String ipAddress, String lastActive, Boolean currentSession) {
        this.userEmail = userEmail;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.location = location;
        this.ipAddress = ipAddress;
        this.lastActive = lastActive;
        this.currentSession = currentSession;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLastActive() {
        return lastActive;
    }

    public void setLastActive(String lastActive) {
        this.lastActive = lastActive;
    }

    public Boolean getCurrentSession() {
        return currentSession != null ? currentSession : false;
    }

    public void setCurrentSession(Boolean currentSession) {
        this.currentSession = currentSession;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
