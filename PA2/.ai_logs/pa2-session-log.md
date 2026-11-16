# PA2 AI Session Log (manual log of the agent session)

> **AI Usage Log:** Kept by the Claude agent (`claude-opus-5-5`, Claude Code-based agentic session in the Claude app) during the session. Student prompts are **verbatim**. Each entry has a date and a summary of the agent's output. Session link (opens only for the owner): https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6

---

## PA2-00 · 2026-11-16 · Scope the updated assignment
**Student prompt (verbatim):**
> Moving forward to project assignment 2. updated instructions are in the folder along with a new readme for dynamically changing instructions. do a scan over and report back with forward steps. chrome access is open for github management

**Agent output:** Read the updated `CS763_ProjectDescription.docx` and `ReadMe.txt`. Summarised the PA2 deliverables (functional-codebase 20%, sast-triage-report 20%, security-analysis 40%, AI usage 20%) and asked for the master spec and the journal template. Listed the sections that must be in the student's own words.

**Student prompt (verbatim):**
> master spec was in wrong folder, in now. i do not have access to conceptual journal template, is it needed? I am keeping track of the due date. the sections that need to be in my own words will be my words, just need you to point when and where

**Agent output:** Read `ProjectPortalDomainServices.docx` (Domain 2 section). Said the template isn't required by the rubric. Gave a table of where the student's own words are needed. Listed what the naive build prompt should cover, with no mention of security.

## PA2-01 · 2026-11-16 · Naive (happy-path) build of the master spec
**Student prompt (verbatim), the research-baseline prompt:**
> I'm working on a project and need you to build the Domain 2 service from ProjectPortalDomainServices.docx.
>
> Go through the doc and implement all 8 endpoints and the 5 functional requirements for Domain 2. Use Java with Spring Boot and a PostgreSQL database.
>
> Put all the code in a new folder called functional-codebase/ in the root of the repo.
>
> What I need:
> - The working code for the service (controllers, services, repositories, entities, database schema)
> - API integration tests for all 8 endpoints, and they all need to pass
> - A Dockerfile and docker-compose.yml so I can run docker compose up and have the service and Postgres running
> - It needs to work with the pipeline that's already in the repo. If the pipeline needs a step for functional-codebase/, add it the same way the other steps are set up.
> - A short README on how to build, run and test it
>
> Keep going until it builds, the containers start and all the tests pass. If anything in the doc is unclear, just make a reasonable call and tell me what you went with at the end.

**Note:** The prompt deliberately contains no security, validation, authentication or encryption requirements (PA2 naive baseline). The agent added none beyond what the prompt asked for.

**Agent output (2026-11-16):**
- Created `functional-codebase/` (now `PA2/functional-codebase/`): a Spring Boot 3.3 / Java 21 / PostgreSQL service with 5 entities (Asset, Ticket, TicketItem, TicketEvent, Allocation), 4 repositories, 3 services, 2 controllers, `schema.sql`, mock data, a multi-stage Dockerfile, docker-compose (service + PostgreSQL), `smoke-test.sh` and a README.
- 19 API integration tests (`AssetApiIntegrationTest` 8, `TicketApiIntegrationTest` 11) covering every route and verb in the spec.
- Pipeline: added `functional-build-test` and `functional-docker` jobs (compose up, smoke test, Trivy fs + image), Semgrep JSON output, and pointed SonarQube Cloud at `functional-codebase/`.
- Agent iterations: **1**. The first push built and passed. The sandbox can't reach Maven Central, so compilation and testing happened only in CI.
- Result, pipeline run [43954567816](https://github.com/azmeerhussain/resource-allocation-helpdesk-hub/actions/runs/43954567816): `Tests run: 19, Failures: 0, Errors: 0`, docker compose smoke test `SMOKE TEST PASSED`, Semgrep ✅, SonarQube Cloud `ANALYSIS SUCCESSFUL`, Trivy ✅ (non-blocking). Log: `PA2/pipeline-logs/pa2-run-43954567816-full.txt`.

**Interpretation choices the agent made (the spec was ambiguous):**
1. The student prompt says "8 endpoints"; the spec table has **7 routes** (14 route + verb operations). All 7 routes and every verb were implemented. No extra endpoint was invented.
2. FR5 "search the asset catalog and ticket logs simultaneously" was implemented as the same filter set (status, category/asset, course, lab) on both GET listing endpoints. There is no combined search endpoint, because the spec's endpoint table doesn't define one.
3. Asset categories: HARDWARE, LAPTOP, IOT_NODE (physical), SOFTWARE_LICENSE, CLOUD_VOUCHER. Each asset belongs to a course **or** a research lab plus a department program.
4. Ticket approval (PUT with `status`) only sets physical assets to RESERVED. **Inventory changes only in `/allocate`** (FR4). `/allocate` runs in a single DB transaction, so all items succeed or none do.
5. Vouchers deduct `voucherAmount` from `voucherBalance`, and the allocation is rejected (409) if the balance would go below zero. Licenses issue the asset's license key. Physical items issue an `HW-` assignment token with a due date (`maxCheckoutDays`, default 14).
6. Overdue tracking is a scheduled job (every 15 min) that flags past-due physical check-outs as OVERDUE. Check-out duration is computed per allocation (`checkoutDurationHours`).
7. Ticket transitions: PENDING→APPROVED/REJECTED, APPROVED→REJECTED, REJECTED→CLOSED, ALLOCATED→CLOSED. ALLOCATED is reachable only through `/allocate`. Only PENDING tickets can be edited or deleted.
8. "Comprehensive logging metrics": every action is logged with SLF4J, ticket state changes are stored as `ticket_event` history, and `GET /tickets/{id}` returns metrics (items, quantities, voucher total, hours open, status changes).
9. `POST /assets/{id}/return` takes an optional `allocationId`. Without one, the oldest outstanding check-out is returned.
10. Requesters, courses, labs and IT directors are external, so they are free-text IDs backed by static mock data (`mock-data/academic-units.json`).

## PA2-02 · 2026-11-16 · Guidance for the human code review
**Student prompt (verbatim):**
> point me to the files that need reviewing too

**Agent output:** A prioritised list of the files in `functional-codebase/` (read closely / skim / quick glance), with line numbers of the logic worth checking and instructions for the header sign-off and the journal.

**Student message (verbatim, start):**
> Code review journal (Nov 16)
> Overall the AI built what I asked for. …

*(The full entry was written by the student and saved verbatim as Entry 1 in `conceptual_journal.md`. The agent only formatted the headings.)*

## PA2-03 · 2026-11-16 · Restructure into a per-assignment folder
**Context:** The student added a note from the instructor to `ReadMe.txt`: "It would be better to create a separate folder for each assignment in your repository to store all relevant deliverables, including your source code, AI logs, and supporting documents."

**Student prompt (verbatim):**
> before we move forward, added something to the readme. read that and get back to me first to see if it changes anything

**Agent output:** Proposed a `PA2/` folder, plus a root `/.ai_logs/` index because the project description still requires that path. Offered two options for PA1: Option A, leave PA1 at the root as submitted, or Option B, move it into `PA1/`.

**Student prompt (verbatim):**
> lets go with option A

**Agent output:** Moved `functional-codebase/`, the PA2 logs and the PA2 pipeline log into `PA2/` with no code changes. Added the root `.ai_logs/README.md` index and `PA2/README.md`, and updated the pipeline paths, file headers and log references.

## PA2-04 · 2026-11-16 · Journal template
**Student prompt (verbatim):**
> before we move forward, forgot a file in the folder that you should go over, its the template for the journal entry that didnt load before. ill do the file headers now

**Agent output:** Read `PA2-Journal.docx` (course template). Restructured `conceptual_journal.md` to the template's sections. Moved the student's Entry 1 verbatim into 2.2, filled Section 1 with factual tool details, added the student's code review as the first row of Section 4, and left every reflection section marked ✍️ for the student.
