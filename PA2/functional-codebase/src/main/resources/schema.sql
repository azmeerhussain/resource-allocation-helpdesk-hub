-- ======================================================================================
-- Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
-- Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
-- Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
--                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
--                      PostgreSQL database" (student prompt PA2-01, verbatim in PA2/.ai_logs/pa2-session-log.md)
-- AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
-- Human Modifications: None
-- Verification       : API integration tests (AssetApiIntegrationTest, TicketApiIntegrationTest) + docker compose smoke test in CI
--                      Human review: PENDING (student to sign off: name, date, what was checked)
-- Confidence         : Medium
-- ======================================================================================
-- Database schema for the Resource Allocation & Help Desk Hub (Domain 2)

CREATE TABLE IF NOT EXISTS asset (
    id                 UUID PRIMARY KEY,
    name               VARCHAR(200),
    category           VARCHAR(30),
    total_stock        INTEGER,
    available_stock    INTEGER,
    status             VARCHAR(30),
    department_program VARCHAR(100),
    course_id          VARCHAR(50),
    lab_id             VARCHAR(50),
    voucher_balance    NUMERIC(12, 2),
    license_key        VARCHAR(255),
    max_checkout_days  INTEGER,
    created_at         TIMESTAMP WITH TIME ZONE,
    updated_at         TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS ticket (
    id             UUID PRIMARY KEY,
    requester_name VARCHAR(200),
    requester_role VARCHAR(50),
    course_id      VARCHAR(50),
    lab_id         VARCHAR(50),
    project_name   VARCHAR(200),
    justification  VARCHAR(2000),
    status         VARCHAR(30),
    reviewed_by    VARCHAR(200),
    review_note    VARCHAR(2000),
    reviewed_at    TIMESTAMP WITH TIME ZONE,
    allocated_by   VARCHAR(200),
    allocated_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS ticket_item (
    id             UUID PRIMARY KEY,
    ticket_id      UUID REFERENCES ticket (id),
    asset_id       UUID,
    quantity       INTEGER,
    voucher_amount NUMERIC(12, 2)
);

CREATE TABLE IF NOT EXISTS ticket_event (
    id          UUID PRIMARY KEY,
    ticket_id   UUID,
    from_status VARCHAR(30),
    to_status   VARCHAR(30),
    actor       VARCHAR(200),
    note        VARCHAR(2000),
    occurred_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS allocation (
    id             UUID PRIMARY KEY,
    ticket_id      UUID,
    asset_id       UUID,
    asset_name     VARCHAR(200),
    category       VARCHAR(30),
    course_id      VARCHAR(50),
    lab_id         VARCHAR(50),
    quantity       INTEGER,
    voucher_amount NUMERIC(12, 2),
    issued_token   VARCHAR(255),
    status         VARCHAR(30),
    checked_out_at TIMESTAMP WITH TIME ZONE,
    due_at         TIMESTAMP WITH TIME ZONE,
    returned_at    TIMESTAMP WITH TIME ZONE
);
