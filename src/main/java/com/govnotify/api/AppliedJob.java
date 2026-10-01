package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applied_jobs")
public class AppliedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false, length = 500)
    private String jobTitle;

    private String organization;
    private String lastDate;

    @Column(length = 500)
    private String officialApplyUrl;

    private LocalDateTime appliedAt;

    public AppliedJob() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getLastDate() { return lastDate; }
    public void setLastDate(String lastDate) { this.lastDate = lastDate; }
    public String getOfficialApplyUrl() { return officialApplyUrl; }
    public void setOfficialApplyUrl(String officialApplyUrl) { this.officialApplyUrl = officialApplyUrl; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
