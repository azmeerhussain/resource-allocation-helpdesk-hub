/*
 * ======================================================================================
 * Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
 * Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
 * Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
 *                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
 *                      PostgreSQL database" (student prompt PA2-01, verbatim in PA2/.ai_logs/pa2-session-log.md)
 * AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
 * Human Modifications: None
 * Verification       : API integration tests (AssetApiIntegrationTest, TicketApiIntegrationTest) + docker compose smoke test in CI
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed fields, returned straight from the API so licenseKey is exposed
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AssetCategory category;

    private Integer totalStock;

    private Integer availableStock;

    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    private String departmentProgram;

    private String courseId;

    private String labId;

    private BigDecimal voucherBalance;

    private String licenseKey;

    private Integer maxCheckoutDays;

    private Instant createdAt;

    private Instant updatedAt;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetCategory getCategory() {
        return category;
    }

    public void setCategory(AssetCategory category) {
        this.category = category;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public String getDepartmentProgram() {
        return departmentProgram;
    }

    public void setDepartmentProgram(String departmentProgram) {
        this.departmentProgram = departmentProgram;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public BigDecimal getVoucherBalance() {
        return voucherBalance;
    }

    public void setVoucherBalance(BigDecimal voucherBalance) {
        this.voucherBalance = voucherBalance;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public Integer getMaxCheckoutDays() {
        return maxCheckoutDays;
    }

    public void setMaxCheckoutDays(Integer maxCheckoutDays) {
        this.maxCheckoutDays = maxCheckoutDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
