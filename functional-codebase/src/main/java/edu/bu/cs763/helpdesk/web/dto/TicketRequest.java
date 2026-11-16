/*
 * ======================================================================================
 * Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
 * Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
 * Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
 *                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
 *                      PostgreSQL database" (student prompt PA2-01, verbatim in .ai_logs/pa2-session-log.md)
 * AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
 * Human Modifications: None
 * Verification       : API integration tests (AssetApiIntegrationTest, TicketApiIntegrationTest) + docker compose smoke test in CI
 *                      Human review: PENDING (student to sign off: name, date, what was checked)
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.web.dto;

import edu.bu.cs763.helpdesk.domain.TicketStatus;
import java.util.List;

/**
 * Used for both creating a ticket (POST) and updating one (PUT).
 * On PUT, a non-null {@code status} performs the approval state transition
 * (APPROVED / REJECTED / CLOSED) and {@code reviewedBy} records who signed off.
 */
public record TicketRequest(
        String requesterName,
        String requesterRole,
        String courseId,
        String labId,
        String projectName,
        String justification,
        List<TicketItemRequest> items,
        TicketStatus status,
        String reviewedBy,
        String reviewNote) {
}
