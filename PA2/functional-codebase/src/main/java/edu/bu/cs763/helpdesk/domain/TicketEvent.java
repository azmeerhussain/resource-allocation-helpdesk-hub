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
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed fields, audit history uses the actor name the caller typed in, so it can be faked
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_event")
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ticketId;

    private String fromStatus;

    private String toStatus;

    private String actor;

    private String note;

    private Instant occurredAt;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

}
