package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "source_registry", indexes = {
    @Index(name = "idx_source_code", columnList = "sourceCode", unique = true)
})
public class SourceRegistryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sourceCode; // e.g. SRC_TNPSC, SRC_TNUSRB

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department; // Optional authoritative department mapping (nullable for multi-dept bodies like TNPSC)

    @Column(nullable = false, length = 100)
    private String state = "Tamil Nadu";

    @Column(nullable = false, length = 200)
    private String sourceName;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 30)
    private String scraperType = "PLAYWRIGHT"; // PLAYWRIGHT, AXIOS, PDF_FEED

    @Column(nullable = false)
    private Integer pollIntervalHours = 6;

    private LocalDateTime lastPolledAt;

    @Column(length = 64)
    private String lastContentHash;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, DEGRADED, FAILED

    @Column(nullable = false)
    private Integer failureCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SourceRegistryEntity() {}

    public SourceRegistryEntity(String sourceCode, Department department, String state, String sourceName, String url, String scraperType) {
        this.sourceCode = sourceCode;
        this.department = department;
        this.state = state;
        this.sourceName = sourceName;
        this.url = url;
        this.scraperType = scraperType;
    }

    public Long getId() { return id; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getScraperType() { return scraperType; }
    public void setScraperType(String scraperType) { this.scraperType = scraperType; }

    public Integer getPollIntervalHours() { return pollIntervalHours; }
    public void setPollIntervalHours(Integer pollIntervalHours) { this.pollIntervalHours = pollIntervalHours; }

    public LocalDateTime getLastPolledAt() { return lastPolledAt; }
    public void setLastPolledAt(LocalDateTime lastPolledAt) { this.lastPolledAt = lastPolledAt; }

    public String getLastContentHash() { return lastContentHash; }
    public void setLastContentHash(String lastContentHash) { this.lastContentHash = lastContentHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getFailureCount() { return failureCount; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
