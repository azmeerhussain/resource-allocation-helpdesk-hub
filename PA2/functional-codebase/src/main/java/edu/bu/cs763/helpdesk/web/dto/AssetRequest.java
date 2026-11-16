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
 *                      Human review: PENDING (student to sign off: name, date, what was checked)
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.web.dto;

import edu.bu.cs763.helpdesk.domain.AssetCategory;
import edu.bu.cs763.helpdesk.domain.AssetStatus;
import java.math.BigDecimal;

public record AssetRequest(
        String name,
        AssetCategory category,
        Integer totalStock,
        Integer availableStock,
        AssetStatus status,
        String departmentProgram,
        String courseId,
        String labId,
        BigDecimal voucherBalance,
        String licenseKey,
        Integer maxCheckoutDays) {
}
