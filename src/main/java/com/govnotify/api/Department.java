package com.govnotify.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_department_code", columnList = "code", unique = true)
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // e.g., TN_REV, TN_POL, TN_EDU

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String state = "Tamil Nadu";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String officialPortalUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Department() {}

    public Department(String code, String name, String state, String description, String officialPortalUrl) {
        this.code = code;
        this.name = name;
        this.state = state;
        this.description = description;
        this.officialPortalUrl = officialPortalUrl;
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOfficialPortalUrl() { return officialPortalUrl; }
    public void setOfficialPortalUrl(String officialPortalUrl) { this.officialPortalUrl = officialPortalUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
