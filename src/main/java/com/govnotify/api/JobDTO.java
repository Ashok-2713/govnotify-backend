package com.govnotify.api;

import java.time.LocalDate;

public class JobDTO {
    private Long id;
    private String title;
    private String state;
    private String centralCategory;
    private String organization;
    private LocalDate lastDate;
    private LocalDate registrationStartDate;
    private String officialApplyUrl;
    private String officialPdfUrl;
    private String status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCentralCategory() { return centralCategory; }
    public void setCentralCategory(String centralCategory) { this.centralCategory = centralCategory; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public LocalDate getLastDate() { return lastDate; }
    public void setLastDate(LocalDate lastDate) { this.lastDate = lastDate; }

    public LocalDate getRegistrationStartDate() { return registrationStartDate; }
    public void setRegistrationStartDate(LocalDate registrationStartDate) { this.registrationStartDate = registrationStartDate; }

    public String getOfficialApplyUrl() { return officialApplyUrl; }
    public void setOfficialApplyUrl(String officialApplyUrl) { this.officialApplyUrl = officialApplyUrl; }

    public String getOfficialPdfUrl() { return officialPdfUrl; }
    public void setOfficialPdfUrl(String officialPdfUrl) { this.officialPdfUrl = officialPdfUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
