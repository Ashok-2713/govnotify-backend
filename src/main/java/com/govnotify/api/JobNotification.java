package com.govnotify.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_notifications", indexes = {
    @Index(name = "idx_job_department_code", columnList = "departmentCode"),
    @Index(name = "idx_job_document_hash", columnList = "documentHash"),
    @Index(name = "idx_job_ref_id", columnList = "notificationReferenceId"),
    @Index(name = "idx_job_source_id", columnList = "source_id")
})
public class JobNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 150)
    private String organization;

    @Column(length = 100)
    private String sector; // e.g., TNPSC, TNUSRB, TRB, MRB, TANGEDCO, Forest, High Court...

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department; // Authoritative department mapping (nullable where specific dept cannot be determined)

    @Column(length = 50)
    private String departmentCode; // Search/compatibility string (e.g. TN_REV, TN_POL)

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = true)
    private SourceRegistryEntity sourceRegistry; // Related source registry entry

    @Column(length = 200)
    private String examName; // e.g., "TNPSC Group 2A", "TNUSRB SI Recruitment"

    @Column(length = 100)
    private String notificationReferenceId; // Alphanumeric official advt code

    @Column(length = 200)
    private String postTitle;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 120)
    private String state; // "Tamil Nadu" or "All-India"

    @Column(name = "central_category", length = 50)
    private String centralCategory; // "Railways", "SSC", "Banking", "India Post", "UPSC", "Other Central"

    @Column(length = 120)
    private String location;

    @Column(length = 200)
    private String qualification;

    @Column(length = 100)
    private String ageLimit;

    private Integer vacancies;

    private LocalDate registrationStartDate;

    private LocalDate lastDate;

    private LocalDate examDate; // Optional exam date if announced

    @Column(length = 20)
    private String status = "Open"; // "Upcoming", "Open", "Closed"

    @Column(length = 500)
    private String salary;

    @Column(length = 500)
    private String officialApplyUrl; // Must be .gov.in or .nic.in / official domain

    @Column(length = 500)
    private String officialPdfUrl;   // Must be .gov.in or .nic.in / official domain

    @Column(length = 500)
    private String sourceUrl;        // Official website URL

    @Column(length = 150)
    private String sourceWebsiteName; // e.g. "www.tnpsc.gov.in"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rawTextSummary;

    @Column(length = 30)
    private String extractionMethod = "HTML_DOM"; // HTML_DOM, PDF_TEXT, OCR_IMAGE, AI_NLP

    @Column(length = 64)
    private String documentHash;

    @Column(nullable = false, length = 20)
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    private Boolean isVerifiedOfficial = false;

    @Column(length = 20)
    private String siteStatus = "HEALTHY"; // HEALTHY, SLOW, DOWN

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column
    private LocalDateTime fetchTimestamp;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @Column(name = "closed_reason", length = 50)
    private String closedReason;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public JobNotification() {
    }

    public JobNotification(String title, String organization, String category, String officialApplyUrl) {
        this.title = title;
        this.organization = organization;
        this.category = category;
        this.officialApplyUrl = officialApplyUrl;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public SourceRegistryEntity getSourceRegistry() { return sourceRegistry; }
    public void setSourceRegistry(SourceRegistryEntity sourceRegistry) { this.sourceRegistry = sourceRegistry; }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public String getNotificationReferenceId() { return notificationReferenceId; }
    public void setNotificationReferenceId(String notificationReferenceId) { this.notificationReferenceId = notificationReferenceId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCentralCategory() { return centralCategory; }
    public void setCentralCategory(String centralCategory) { this.centralCategory = centralCategory; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getAgeLimit() { return ageLimit; }
    public void setAgeLimit(String ageLimit) { this.ageLimit = ageLimit; }

    public Integer getVacancies() { return vacancies; }
    public void setVacancies(Integer vacancies) { this.vacancies = vacancies; }

    public LocalDate getRegistrationStartDate() { return registrationStartDate; }
    public void setRegistrationStartDate(LocalDate registrationStartDate) { this.registrationStartDate = registrationStartDate; }

    public LocalDate getLastDate() { return lastDate; }
    public void setLastDate(LocalDate lastDate) { this.lastDate = lastDate; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getOfficialApplyUrl() { return officialApplyUrl; }
    public void setOfficialApplyUrl(String officialApplyUrl) { this.officialApplyUrl = officialApplyUrl; }

    public String getOfficialPdfUrl() { return officialPdfUrl; }
    public void setOfficialPdfUrl(String officialPdfUrl) { this.officialPdfUrl = officialPdfUrl; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceWebsiteName() { return sourceWebsiteName; }
    public void setSourceWebsiteName(String sourceWebsiteName) { this.sourceWebsiteName = sourceWebsiteName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRawTextSummary() { return rawTextSummary; }
    public void setRawTextSummary(String rawTextSummary) { this.rawTextSummary = rawTextSummary; }

    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }

    public String getDocumentHash() { return documentHash; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public Boolean getIsVerifiedOfficial() { return isVerifiedOfficial; }
    public void setIsVerifiedOfficial(Boolean isVerifiedOfficial) { this.isVerifiedOfficial = isVerifiedOfficial; }

    public String getSiteStatus() { return siteStatus; }
    public void setSiteStatus(String siteStatus) { this.siteStatus = siteStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getFetchTimestamp() { return fetchTimestamp; }
    public void setFetchTimestamp(LocalDateTime fetchTimestamp) { this.fetchTimestamp = fetchTimestamp; }

    public LocalDateTime getStatusUpdatedAt() { return statusUpdatedAt; }
    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }

    public String getClosedReason() { return closedReason; }
    public void setClosedReason(String closedReason) { this.closedReason = closedReason; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
}
