# Security Analysis: Resource Allocation & Help Desk Hub (PA2)

**Service:** Domain 2, Resource Allocation & Help Desk Hub (`ProjectPortalDomainServices.docx`) · **Code analysed:** `PA2/functional-codebase/` (AI-generated naive baseline) · **Author:** Azmeer Hussain

## AI Deliverable Metadata Block

| Item | Value |
|---|---|
| Conceptual AI contribution | **Part 1 (Security requirements): ~90% AI / ~10% human.** Later parts are added below as they are written. |
| Human (student) | Wrote the prompt for each part (multi-turn, part-by-part chaining). Supplied the confirmed flaw set M1–M8 (`sast-triage-report.md`), where the classifications and rationales are the student's. Audits each AI draft (see `.ai_logs/conceptual_journal.md` §3). |
| AI (Claude, `claude-opus-5-5`) | Drafted the text of each part from the student's prompt, the master spec, the generated code and M1–M8. |
| Prompts (verbatim) | [`.ai_logs/pa2-session-log.md`](.ai_logs/pa2-session-log.md), entries PA2-09 onward |

---

## 1. Security Requirements (defensive baseline)

**Prompt (PA2-09):** *"Can you write the security requirements for the Domain 2 service? Use the spec, the code and the M1–M8 flaws we found. Number them SR-1, SR-2, etc., and say which M flaw each one fixes."*

**Roles assumed by these requirements** (derived from the master spec, since the naive code has no roles):

| Role | Who (spec) | Allowed actions |
|---|---|---|
| `REQUESTER` | Instructors and faculty advisors (FR3) | Submit tickets; view, edit and delete **their own** PENDING tickets; view allocations for **their own** course or lab |
| `IT_DIRECTOR` | IT director sign-off (FR4) | Approve or reject tickets; view all tickets and allocations |
| `IT_ADMIN` | IT staff running inventory (FR1, FR2) | Manage the asset catalog; execute `/allocate` on APPROVED tickets; process returns |

### 1.1 Requirements

| ID | Requirement | Fixes | How it can be verified (PA3/PA4) |
|---|---|---|---|
| **SR-1** | **Authentication on every endpoint.** Every `/api/v1/assets/**` route requires a valid JWT signed with an **asymmetric** key (RS256/ES256), with `iss`, `aud` and `exp` checked. Only `/actuator/health` is public. Missing, expired, forged or `alg=none` tokens get `401`. | **M2**, M6 | Call each route with no token, an expired token, a token signed with the wrong key, and `alg=none` → all `401` |
| **SR-2** | **Role-based access control, deny by default.** Each route is limited to the roles in the table above. Any action not explicitly allowed is denied with `403`. | **M2** | A `REQUESTER` token calling PUT approve, `/allocate`, or asset POST/PUT/DELETE → `403` |
| **SR-3** | **Object-level authorization (no BOLA/IDOR).** A `REQUESTER` can read or change only tickets they created and allocations for course/lab IDs in their token claims. Ownership comes from the token, never from request parameters. | **M2**, M4 | Requester A fetches requester B's ticket ID, or `GET /allocations?courseId=<other course>` → `403`/`404` |
| **SR-4** | **Separation of duties for approval vs. allocation (FR4).** Only `IT_DIRECTOR` can approve or reject. Only `IT_ADMIN` can execute `/allocate`, and only on `APPROVED` tickets. The approver cannot be the requester of the same ticket. | **M2**, M6 | Director approving their own ticket → `403`; `/allocate` on a PENDING ticket → `409` |
| **SR-5** | **Server-derived identity and a tamper-evident audit trail.** `requesterName`, `reviewedBy`, `allocatedBy` and every `ticket_event.actor` come from the authenticated principal. Any client-supplied value for these fields is ignored. Audit events are append-only and are **not** deleted when a ticket is deleted. | **M6** | Send `"reviewedBy":"IT Director"` with a different user's token → the stored value is that user's identity; delete a ticket → its events remain |
| **SR-6** | **Strict input validation.** `quantity` is an integer from 1 to a configured maximum (e.g. 50). `voucherAmount` is greater than 0, at most a configured cap, with 2 decimal places. Enums are whitelisted, string fields are length-limited, a ticket has 1–20 items, and unknown JSON fields are rejected. Violations → `400` with no state change. | **M1** | Quantities `-5`, `0`, `1000000` and amounts `-1000`, `0.001` → `400`; stock and balance unchanged (repeat of the PA2 manual test) |
| **SR-7** | **Inventory invariants enforced in the database (defence in depth).** `CHECK (available_stock >= 0 AND available_stock <= total_stock)`, `CHECK (voucher_balance >= 0)`, `CHECK (quantity > 0)`, `NOT NULL` on required columns, and foreign keys `allocation → asset/ticket`, `ticket_item → asset`. | **M1**, M3 | A direct SQL update to negative stock fails; the app returns `409`, not `500` |
| **SR-8** | **Atomic, concurrency-safe allocation.** `/allocate` locks every affected asset row (`SELECT … FOR UPDATE`) or uses optimistic locking (`@Version`) with a bounded retry. All items succeed or none do. A ticket can be allocated **once** (idempotent). | **M3** | Fire N parallel `/allocate` calls for tickets competing for the last unit → exactly one succeeds; stock never below 0 |
| **SR-9** | **No mass assignment on assets.** The asset update API cannot set `availableStock`, `status` or `licenseKey` directly. Status is derived by the server. Stock changes go through dedicated, audited adjustment operations that keep SR-7's invariants. An asset with outstanding allocations cannot be deleted. | M1, M2 *(also covers the student's review note that "you can set the stock or status to anything", which is not one of M1–M8)* | `PUT /assets/{id}` with `availableStock:-1` or `status:AVAILABLE` → ignored or `400`; delete an asset with an active allocation → `409` |
| **SR-10** | **Protection of license keys and voucher codes.** Secret values are **encrypted at rest** (AES-256-GCM, key from the environment or a secret store, not in the repo). They are **never** included in list, detail or summary responses. The owning requester can reveal them only through a dedicated, audited endpoint. Each license allocation issues a unique seat token, not the shared master key. | **M4** | `GET /allocations`, `GET /tickets/{id}` and `GET /assets/{id}` contain no key material; DB dump shows ciphertext; a reveal writes an audit event |
| **SR-11** | **Logging hygiene and security event logging.** Logs never contain license keys, voucher codes, tokens or credentials (mask or omit). Security events are logged with actor and trace ID: login failures, `403`s, approvals, rejections, allocations, returns, secret reveals. | **M4**, M6 | Grep the logs after a full allocate flow → no key material; approvals show the authenticated actor |
| **SR-12** | **Server-enforced workflow state machine.** Only the defined transitions are allowed (PENDING→APPROVED/REJECTED, APPROVED→REJECTED, APPROVED→ALLOCATED via `/allocate` only, ALLOCATED/REJECTED→CLOSED). **APPROVED→REJECTED must release the reservations** the approval created, and reservations expire if not allocated within a configured window. | **M5** | Approve then reject → the asset returns to `AVAILABLE` and reappears in `GET /assets?status=AVAILABLE` |
| **SR-13** | **Safe error handling.** Clients get a generic message, a stable error code and a correlation ID. No internal UUIDs of other objects, stock or balance figures, SQL or stack traces. Details go only to the server log. | **M7** | Trigger insufficient-stock, invalid-transition and not-found errors → the response body has no numbers or IDs beyond the requested resource |
| **SR-14** | **No hard-coded or default credentials.** Database credentials and signing/encryption keys come from environment variables or a secret store, with **no default values**. The service refuses to start if they're missing. PostgreSQL is not published on a host port. | **M8** | Start without `DB_PASSWORD` → startup fails; `docker compose config` shows no literal password; `5432` not exposed |
| **SR-15** | **Least-privilege container.** The runtime image runs as a non-root user with a minimal base image and no build tools. | S4 | `docker exec … id` → non-zero UID; Semgrep `missing-user-entrypoint` clears |
| **SR-16** | **Supply-chain integrity.** CI actions are pinned to full commit SHAs. Dependencies (Spring Boot and its transitive libraries) are kept on supported, patched versions, and dependency scanning runs on every build. | S1, Trivy appendix | Semgrep `mutable-action-tag` clears; Trivy shows no CRITICAL findings with a fix available |
| **SR-17** | **Transport security.** In any non-local deployment the API is served only over HTTPS (TLS 1.2+) and tokens are never sent over plain HTTP. | Baseline (no M; the spec involves high-value tokens) | `curl http://…` refused or redirected; TLS scan |
| **SR-18** | **Abuse and resource limits.** Rate limits on ticket submission, `/allocate` and secret reveal per user; a cap on items per ticket and on page size for list endpoints. | Baseline (no M); limits impact of M1/M3-style abuse | Burst of 100 ticket submissions → `429` after the limit |

### 1.2 Traceability: every confirmed flaw is covered

| Flaw | Covered by |
|---|---|
| M1 Negative quantity / voucher amount (CWE-1284) | SR-6, SR-7, SR-9 |
| M2 No authentication or authorization (CWE-306/862) | SR-1, SR-2, SR-3, SR-4, SR-9 |
| M3 Allocation race condition (CWE-362) | SR-8, SR-7 |
| M4 License keys exposed in responses and logs (CWE-200/532) | SR-10, SR-11, SR-3 |
| M5 Reservation not released on reject (CWE-841) | SR-12 |
| M6 Spoofable approver/actor identity (CWE-290) | SR-5, SR-1, SR-4, SR-11 |
| M7 Verbose error messages (CWE-209) | SR-13 |
| M8 Hard-coded / default DB credentials (CWE-798/1393) | SR-14 |
| S1 Mutable CI action tags (CWE-1357) | SR-16 |
| S4 Container runs as root (CWE-250) | SR-15 |

---

## 2. Abuse Cases
*(next part, pending the student's prompt)*

## 3. Attack Trees (≥ 3 abuse cases)
*(pending)*

## 4. Attack Surface Map
*(pending)*

## 5. Data Flow Diagram & STRIDE (every component)
*(pending)*

## 6. Proposed Mitigations
*(pending)*
