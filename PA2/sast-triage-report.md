# SAST Triage Report: PA2 Naive Baseline

**Service:** Resource Allocation & Help Desk Hub (Domain 2) · **Code:** `PA2/functional-codebase/` (Java 21 / Spring Boot 3.3 / PostgreSQL 16) · **Author:** Azmeer Hussain · **Date:** 2026-11-16

## AI Deliverable Metadata Block

| Item | Value |
|---|---|
| Conceptual AI contribution | **~60% AI / ~40% human** |
| Human (student) | Every TP/FP classification and every "confirm" decision, with the rationale **in his own words** (the "Student rationale" column, verbatim). Manual exploit of M1 (`PA2/evidence/`). Code review that surfaced M2–M8 (`.ai_logs/conceptual_journal.md` 2.2, file headers). |
| AI (Claude, `claude-opus-5-5`) | Ran and exported the scans, compiled the raw ledger, proposed candidate classifications and CWE mappings (the student accepted or changed them), and wrote the technical root-cause and location text and this document's structure. |
| Raw evidence | Pipeline run [44004990430](https://github.com/azmeerhussain/resource-allocation-helpdesk-hub/actions/runs/44004990430), log: [`pipeline-logs/pa2-run-44004990430-sast-baseline.txt`](pipeline-logs/pa2-run-44004990430-sast-baseline.txt) |
| AI history | [`.ai_logs/pa2-session-log.md`](.ai_logs/pa2-session-log.md) (PA2-07, PA2-08) |

---

## 1. Scan configuration

| Tool | Type | Configuration | Target |
|---|---|---|---|
| **Semgrep** | SAST | `p/java`, `p/secrets`, `p/dockerfile`, `p/security-audit`, `p/owasp-top-ten` via [`scripts/sast-scan.sh`](../scripts/sast-scan.sh). The first baseline run used only the first three rulesets and found 1 issue; the rules were broadened so the triage covers a wider alert set. | Whole repository (logs and evidence excluded via `.semgrepignore`) |
| **SonarQube Cloud** | SAST | Quality profile "Sonar way core" (313 Java rules). Findings exported by the pipeline step *Export SonarQube Cloud findings*. | `PA2/functional-codebase/` (31 files, 1,433 LOC) |
| **Trivy 0.57.1** | SCA / container | `fs` (vuln, secret, misconfig) + `image` scan | `PA2/functional-codebase/` and the built image |

## 2. Raw output summary

| Tool | Raw findings | True positives | False positives |
|---|---|---|---|
| Semgrep | **21** (18× mutable action tag, 2× CWE-78 in the CI workflow, 1× Dockerfile root user) | 20 (S1 ×18, S2, S4) | 1 (S3) |
| SonarQube Cloud | **0 issues, 0 security hotspots** (quality gate fails only on coverage: 0% < 80%) | n/a | n/a |
| Trivy (appendix) | 4 HIGH in `pom.xml` deps, 1 MEDIUM misconfig, **89** in the image jar (9 CRITICAL, 35 HIGH) | see Appendix A | n/a |

**Key observation:** neither SAST tool found a single issue in the Java application code. Every application-logic vulnerability below (M1–M8) was found by **manual review and manual testing**.

---

## 3. Scanner findings ledger (true / false positive)

| ID | Tool · rule | Location | CWE | Verdict | Student rationale (verbatim) | Technical root cause |
|---|---|---|---|---|---|---|
| **S1** | Semgrep · `yaml.github-actions.security.github-actions-mutable-action-tag` (18 findings) | `.github/workflows/devsecops.yml` (every `uses: …@v4`) | CWE-1357 (tool) / CWE-829 | ✅ **TP: low** | "The pipeline uses actions by version tag instead of an exact version, so if one of them got hacked, the bad code would run in our pipeline. It’s real but low risk." | Third-party actions are referenced by mutable tags (`@v4`), not immutable commit SHAs. A re-pointed tag runs attacker code with the job's token and secrets (e.g. `SONAR_TOKEN`). |
| **S2** | Semgrep · `yaml.github-actions.security.run-shell-injection` | `devsecops.yml`, Semgrep step (`${{ github.event.inputs.semgrep_target }}` in `run:`) | CWE-78 | ✅ **TP: low, fixed** | "When the scan ran, the input was pasted straight into the shell script, so the scanner was right. I’ve since changed it to pass through a variable. It’s low severity anyway because only people with write access can run it." | The workflow-dispatch input was expanded by the Actions template engine **before** the shell ran, so shell metacharacters in the input would execute. This was introduced by the AI agent while adding the input. Fixed in commit `5456be5` (value now passed via `env: SEMGREP_TARGET`). |
| **S3** | Semgrep · `yaml.github-actions.security.gha-curl-pipe-shell` | `devsecops.yml:274` (SonarQube export) | CWE-78 | ❌ **FP** | "The downloaded data just gets read to check a status. Nothing downloaded is ever run, so this isn’t really a problem." | The rule matches `curl … \| <interpreter>`. Here curl's JSON output is piped into a **fixed** `python3 -c` program that only parses it (`json.load`). The downloaded data is data, never code. |
| **S4** | Semgrep · `dockerfile.security.missing-user-entrypoint` | `PA2/functional-codebase/Dockerfile:28` | CWE-269 (tool) / CWE-250 | ✅ **TP: medium** | "The container runs as root, so if someone broke into the app they’d have full control of the container. My PA1 version didn’t have this problem." | The runtime stage has no `USER` instruction, so the JVM runs as UID 0. Any RCE (e.g. the Tomcat CVE in Appendix A) gets root inside the container. |
| n/a | SonarQube Cloud | `PA2/functional-codebase/` | n/a | **N/A** | "SonarQube found 0 issues and 0 hotspots. That’s a finding in itself, because it missed every logic flaw in M1–M8." | The pattern-based rules in "Sonar way core" have no model of the business rules (stock bounds, approval separation, ownership). The code uses Spring Data and JPA Criteria (no string-built SQL), so no injection or taint patterns match. |
| n/a | Trivy (SCA) | Spring Boot 3.3.5 dependency tree | various CVEs | **TP (appendix)** | "The Spring Boot version is out of date and uses libraries with known security issues. Upgrading should fix most of them, though not all of them actually affect this app." | The agent pinned Spring Boot **3.3.5** (Oct 2024), which pulls in Tomcat, Jackson, Logback and pgjdbc versions with published CVEs. See Appendix A. |

---

## 4. Confirmed vulnerabilities (manual hunt: flaws the scanners missed)

At least 3 distinct, multi-layered vulnerabilities were required; the student confirmed **8**, across 7 software layers.

| ID | Layer | Vulnerability | CWE | Student rationale (verbatim) | Technical root cause | Location |
|---|---|---|---|---|---|---|
| **M1** | **Input validation** | Negative quantity or voucher amount **increases** stock or balance | **CWE-1284** Improper Validation of Specified Quantity in Input | "I tested this myself. Requesting -5 laptops made the available count go up from 10 to 15. The fix is to reject any quantity or voucher amount that’s zero or less." | `quantity` and `voucherAmount` are accepted as any integer or decimal. `allocate()` computes `available - quantity` and `balance - amount`; with a negative value both checks (`available < quantity`, `remaining.signum() < 0`) pass and the subtraction **adds** inventory. **Exploited by hand** (`PA2/evidence/`). | `TicketService.java:277–281` (stock), `:268–272` (voucher), `:318` (no bound on input) |
| **M2** | **Authentication / Authorization** | No authentication and no roles: any caller can approve, allocate, edit, delete or return | **CWE-306** Missing Authentication for Critical Function, **CWE-862** Missing Authorization | "There’s no login or roles, so anyone can approve, allocate, edit or delete. This gets fixed by the JWT and role checks PA3 already requires." | No Spring Security in the build (`pom.xml`), and controllers expose every route unauthenticated. The FR4 rule "IT director sign-off" is only a status value in the request body, and no object-level ownership check exists (BOLA). | `AssetController.java`, `TicketController.java` (all routes), `pom.xml` |
| **M3** | **Database / Concurrency** | Race condition: concurrent allocations can overspend stock or vouchers | **CWE-362** Race Condition (also **CWE-367** TOCTOU) | "If two requests come in at the same time, both can pass the stock check and take more than what’s there. The fix is to lock the asset row while allocating." | `allocate()` reads the asset (`findById`), checks it, then writes `available - quantity` with no row lock, optimistic `@Version` or DB `CHECK` constraint. Under PostgreSQL's default READ COMMITTED, two transactions read the same value, so one deduction is lost (lost update) and stock can go below the true remaining amount. | `TicketService.java:249` (read), `:277–281` (check then write) |
| **M4** | **Data exposure / Logging** | License keys returned in API responses and written to logs | **CWE-200** Exposure of Sensitive Information, **CWE-532** Sensitive Information in Log File | "License keys come back in the allocations list and the ticket details, and they get printed in the logs. The fix is to hide them from those responses and stop logging them." | JPA entities are serialised directly as responses (`Asset.licenseKey`, `Allocation.issuedToken`). `GET /allocations` aggregates every key (`assignedSoftwareKeys`). Keys are logged on create and on allocation. Every license allocation also issues the **same** master key. | `AllocationService.java:67`, `TicketService.java:283, 295`, `AssetService.java:70`, `TicketDetails`/`AllocationSummary` DTOs |
| **M5** | **State machine** | Rejecting an APPROVED ticket never releases the RESERVED flag on its assets | **CWE-841** Improper Enforcement of Behavioral Workflow | "Rejecting an approved ticket leaves its assets stuck as reserved, so nobody else can request them. The fix is to set them back to available when a ticket is rejected." | `transition()` sets physical assets to `RESERVED` on APPROVED but has no reverse step for `APPROVED → REJECTED`. The flag is only recalculated by a later allocation or return. Effect: the asset disappears from `GET /assets?status=AVAILABLE` and shows a wrong status indefinitely. (`allocate()` checks stock, not status, so direct allocation still works.) | `TicketService.java:192–220` (`transition()`, `:213`) |
| **M6** | **Authorization / Audit integrity** | Approver, allocator and audit-history identities are caller-supplied text | **CWE-290** Authentication Bypass by Spoofing (impact: **CWE-778**-style untrustworthy audit trail) | "The approver and allocator names are just text the caller types in, so anyone can pretend to be the IT Director in the history. Same fix as M2: take the name from the logged-in user instead." | `reviewedBy`, `allocatedBy`, `requesterRole` come from the JSON body and are stored as-is in `ticket` and `ticket_event`. Deleting a PENDING ticket also deletes its event history (`deleteByTicketId`). | `TicketService.java:200, 301`, `TicketEventRepository.deleteByTicketId` |
| **M7** | **API / Error handling** | Error responses leak internal identifiers and inventory figures | **CWE-209** Generation of Error Message Containing Sensitive Information | "Error messages send back internal IDs and stock or balance numbers. The fix is to send back a simple message and keep the details in the logs." | `ApiExceptionHandler` returns `exception.getMessage()` verbatim, and `ConflictException` messages embed asset UUIDs, available stock and voucher balance (e.g. "Insufficient voucher balance on asset … balance 100.00"), which lets an attacker enumerate inventory. | `ApiExceptionHandler.java:43`, `TicketService.java` / `AssetService.java` exception messages |
| **M8** | **Configuration / Deployment** | Hardcoded and default database credentials | **CWE-798** Use of Hard-coded Credentials, **CWE-1393** Use of Default Password | "The database password is typed straight into docker-compose, and application.yml has a default password. The fix is to load it from an environment variable with no default." | `POSTGRES_PASSWORD: helpdesk` and `DB_PASSWORD: helpdesk` in compose (with PostgreSQL also published on host port 5432). `application.yml` falls back to `${DB_PASSWORD:helpdesk}`. | `docker-compose.yml:19, 21, 39`, `application.yml:19` |

### Layer coverage
Input (M1) · Authentication/Authorization (M2, M6) · Database/Concurrency (M3) · Data exposure/Logging (M4) · State machine (M5) · API/Error handling (M7) · Configuration/Deployment (M8, S4) · CI/CD supply chain (S1, S2)

### PA3 remediation scope created by this report
Per the course ReadMe, PA3 must remediate **all confirmed CWEs** in this report: S1, S4, M1–M8. S2 is already fixed. M2 and M6 are covered by the JWT authentication and role/ownership checks that PA3 already requires. Appendix A (dependency CVEs) is tracked separately, and a Spring Boot upgrade resolves most of it.

---

## Appendix A: Dependency and container findings (Trivy, informational)

Run 44004990430, job *functional-codebase: Docker Compose, Smoke Test & Trivy*.

| Scan | Total | Breakdown |
|---|---|---|
| `fs` → `pom.xml` dependencies | 4 | 4 HIGH (`org.postgresql:postgresql` 42.7.4, e.g. CVE-2025-49146) |
| `fs` → misconfiguration | 1 | 1 MEDIUM |
| `image` → `app.jar` libraries | 89 | 9 CRITICAL, 35 HIGH, 31 MEDIUM, 14 LOW |

Representative image findings: `tomcat-embed-core` 10.1.31 **CVE-2025-24813 (CRITICAL)**, `jackson-databind` CVE-2026-54512 (HIGH), `logback-core` 1.5.11 CVE-2024-12798, `pgjdbc` 42.7.4 CVE-2025-49146 (HIGH), `spring-boot` 3.3.5 CVE-2025-22235. **Root cause:** the agent pinned Spring Boot 3.3.5 (Oct 2024). **Remediation:** upgrade the Spring Boot parent to a currently supported release, then re-scan. As the student notes, not every CVE is reachable in this app; reachability is not assessed here.
