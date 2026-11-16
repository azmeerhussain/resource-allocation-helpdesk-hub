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
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed status values, nothing wrong with them
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.domain;

public enum AssetCategory {
    HARDWARE,
    LAPTOP,
    IOT_NODE,
    SOFTWARE_LICENSE,
    CLOUD_VOUCHER;

    public boolean isPhysical() {
        return this == HARDWARE || this == LAPTOP || this == IOT_NODE;
    }
}
