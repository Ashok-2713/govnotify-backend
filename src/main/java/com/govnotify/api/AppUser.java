package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String district;

    // Notification Preferences
    @Column
    private Boolean newJobAlerts = true;

    @Column
    private Boolean upcomingJobAlerts = true;

    @Column
    private Boolean applicationUpdates = true;

    @Column
    private Boolean newsAnnouncements = false;

    @Column
    private Boolean emailNotifications = true;

    // Security Settings
    @Column
    private Boolean twoFactorEnabled = false;

    // Appearance Preferences
    @Column(length = 20)
    private String themeMode = "light";

    @Column(length = 20)
    private String fontSize = "medium";

    @Column(length = 20)
    private String language = "en";

    @Column(name = "reset_otp", length = 10)
    private String resetOtp;

    @Column(name = "reset_otp_expires")
    private LocalDateTime resetOtpExpires;

    @Column(name = "last_notifications_read_at")
    private LocalDateTime lastNotificationsReadAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Boolean getNewJobAlerts() {
        return newJobAlerts != null ? newJobAlerts : true;
    }

    public void setNewJobAlerts(Boolean newJobAlerts) {
        this.newJobAlerts = newJobAlerts;
    }

    public Boolean getUpcomingJobAlerts() {
        return upcomingJobAlerts != null ? upcomingJobAlerts : true;
    }

    public void setUpcomingJobAlerts(Boolean upcomingJobAlerts) {
        this.upcomingJobAlerts = upcomingJobAlerts;
    }

    public Boolean getApplicationUpdates() {
        return applicationUpdates != null ? applicationUpdates : true;
    }

    public void setApplicationUpdates(Boolean applicationUpdates) {
        this.applicationUpdates = applicationUpdates;
    }

    public Boolean getNewsAnnouncements() {
        return newsAnnouncements != null ? newsAnnouncements : false;
    }

    public void setNewsAnnouncements(Boolean newsAnnouncements) {
        this.newsAnnouncements = newsAnnouncements;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications != null ? emailNotifications : true;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled != null ? twoFactorEnabled : false;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getThemeMode() {
        return themeMode != null ? themeMode : "light";
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public String getFontSize() {
        return fontSize != null ? fontSize : "medium";
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getLanguage() {
        return language != null ? language : "en";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getResetOtp() {
        return resetOtp;
    }

    public void setResetOtp(String resetOtp) {
        this.resetOtp = resetOtp;
    }

    public LocalDateTime getResetOtpExpires() {
        return resetOtpExpires;
    }

    public void setResetOtpExpires(LocalDateTime resetOtpExpires) {
        this.resetOtpExpires = resetOtpExpires;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastNotificationsReadAt() {
        return lastNotificationsReadAt;
    }

    public void setLastNotificationsReadAt(LocalDateTime lastNotificationsReadAt) {
        this.lastNotificationsReadAt = lastNotificationsReadAt;
    }
}
