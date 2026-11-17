# MASTER CONCEPTUAL JOURNAL & AI TELEMETRY LOG

> **AI Deliverable Metadata Block**
> | Item | Value |
> |---|---|
> | Author | Azmeer Hussain |
> | Conceptual AI contribution | **~0% of the reflection text (Sections 2–4).** Those are written by the student. The AI agent (Claude, `claude-opus-5-5`) set up this file from the course template (`PA2-Journal.docx`), filled in Section 1 (factual tool details) and pasted the student's entries in verbatim. |
> | AI involvement in what is reflected on | The code being reviewed was 100% AI-generated (PA2-01). For Entry 1, the agent pointed the student to the files and line ranges worth reviewing (`pa2-session-log.md` in this folder, PA2-02). The review and the opinions are the student's. |

- **Student Name:** Azmeer Hussain
- **Assigned Domain Module:** Domain 2: Resource Allocation & Help Desk Hub
- **Core Technology Stack:** Java 21 / Spring Boot 3.3 / Spring Data JPA / PostgreSQL 16 / Maven / Docker Compose

---

## SECTION 1: SYSTEM ENVIRONMENT & TOOL SPECIFICATION

### 1.1 AI Tool & Engine Deployments

| Tool Context | Framework / Platform Name & Version | Underlying LLM Brain Model String | Operational Setting / Access Mode |
|---|---|---|---|
| **Code Generation** | Claude (Anthropic) agentic session on the Claude Code agent harness (Claude Agent SDK), used from the Claude desktop/web app | `claude-opus-5-5` (configured model; the vendor runtime may fall back) | Remote, isolated Linux cloud sandbox (Ubuntu 24.04) with shell/file tools. Pushes to GitHub through the Claude GitHub App. Browser actions run in the student's Chrome through Claude in Chrome. The student approves high-impact actions. Builds and tests run in GitHub Actions. |
| **Document/Design** | Same Claude agentic session (no separate chat tool used) | `claude-opus-5-5` | Same as above |

### 1.2 Verification Telemetry Access Checklist

- [x] Primary session logs saved to the `/.ai_logs/` directory: root index [`/.ai_logs/README.md`](../../.ai_logs/README.md) → [`pa2-session-log.md`](pa2-session-log.md) (manual log with dated entries, verbatim prompts and an output summary for each prompt)
  > External Chat Cloud Share Link 1: Not used. A manual session log with dated entries and verbatim prompts is provided instead ([`pa2-session-log.md`](pa2-session-log.md)), as the telemetry rule allows when no exported report is available.
  > Session URL (owner access): https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6

---

## SECTION 2: COGNITIVE REFLECTION: CODE GENERATION & COMPREHENSION

### 2.1 The Architectural Flow (How You Understand the Code)

The code uses a layered MVC setup, going from controller to service to repository. For POST /tickets/{id}/allocate, TicketController receives the JSON and passes it to TicketService.allocate(), which checks the ticket is APPROVED. For each item, it loads the asset, checks the stock or balance, takes it off, creates an Allocation and issues a token. Everything is saved through the Spring Data repositories (the Repository pattern) inside one @Transactional block, so if anything fails, it all gets rolled back. Requests and responses use DTOs written as Java records, and Spring passes everything in through dependency injection.

### 2.2 Defensive Edge-Case Evaluation (Line-by-Line Code Review)

#### Entry 1 · 2026-11-16 · Code review of the AI-generated functional codebase (written by the student)

Overall the AI built what I asked for. The endpoints work, the tests pass and Docker runs. But I never mentioned security, so it didn’t add any, and that shows up all over the code.

**TicketService:** This had the biggest problems. If two people request the same asset at the same time, the stock can go below zero. Every allocation gets the same license key, and the key gets printed in the logs. When a ticket is rejected, the reserved assets never get released.

**AssetService:** You can set the stock or status to anything, even negative numbers. You can also delete an asset that someone is still using.

**AllocationService:** Anyone can see every license key just by calling one endpoint.

**Controllers:** There’s no login or role check, so anyone can approve tickets or hand out assets.

**Database:** There are no rules to stop bad data, like negative stock or missing fields.

**Other stuff:** The passwords are hardcoded, the error messages show too much, and the container runs as root, which my PA1 version didn’t.

**Tests:** They only check that things work when used normally. Nothing tests what happens when someone misuses the API.

**Do I agree with it?** Partly. The code is clean and easy to follow, but a lot of these issues are things a good developer would catch without being told. The AI focused on getting it working and passing tests, not on making it safe.

### 2.3 The AI Agent's "Security Sins" (Evaluating Code Deficiencies)

My prompt only asked for working code and passing tests, so the AI did exactly that and nothing more. It trusted every input, so a negative quantity added stock. It made approval just a field in the request, so anyone can approve their own ticket. It checked stock and then updated it with no lock, it returned database objects directly and logged the license keys, and it only tested normal use. Later it even added a shell injection risk to the pipeline (S2), which Semgrep caught.

---

## SECTION 3: COGNITIVE REFLECTION: DOCUMENT AUTHORING & THREAT DESIGN

### 3.1 AI Collaboration Matrix for Security Analysis

I wrote six prompts, one for each section, and sent them one at a time so I could check each part before moving on. Each prompt kept the AI to the spec, the code and the M1–M8 flaws, and asked for specific formats like SR numbering, Mermaid and tables. All of them are recorded word for word in pa2-session-log.md (PA2-09 to PA2-14).

### 3.2 Evaluation of Automated Conceptual "Blind Spots"

Most of the "works today" claims in the abuse cases came from the AI reading the code, and only AC-1 was actually tested by hand. Some parts were generic. SR-17 (HTTPS) and SR-18 (rate limits) are good advice but aren't tied to any flaw we found. In Tree 2, I checked the code and one branch is already blocked, because only PENDING tickets can be edited. Another branch only applies after PA3 adds login. The AI also counted 8 endpoints because of my prompt, when the spec only has 7 routes, so it padded the list with the actuator endpoint.

---

## SECTION 4: HUMAN VERIFICATION & AUDIT TRAIL LOG

*Every manual test, integration check, dynamic verification (Postman, cURL) or scan triage loop done by the student by hand.*

| Timestamp | Component Target | Testing/Auditing Methodology | Verified Outcome / Discovered Defect |
|---|---|---|---|
| 2026-11-16 | `PA2/functional-codebase/` (TicketService, AssetService, AllocationService, controllers, schema, Docker config) | Manual code review of the AI-generated source, guided by the agent's file/line list (PA2-02) | Found missing concurrency control on allocation, a shared license key that is also logged, no authorization, unconstrained asset updates, and more (see 2.2, Entry 1) |
| 2026-11-16 | `POST /api/v1/assets/tickets/{id}/allocate` (run by hand with curl in GitHub Codespaces; screenshots in `PA2/evidence/`) | Negative quantity test | Negative quantity test - I requested -5 laptops and the system accepted it. Instead of going down, the number of available laptops went up from 10 to 15, even though there are only 10 in total. Anyone could use this to make the system think there’s more stock than there really is. |
| 2026-11-16 | Semgrep and SonarQube results (sast-triage-report.md) | Went through all 21 Semgrep alerts and checked each one against the workflow and Dockerfile, then checked SonarQube's result | 20 were real and 1 was a false positive (S3, the curl output is only read as JSON and never run). SonarQube found 0 issues, which means it missed every logic flaw in M1–M8. |
| 2026-11-16 | PA2/security-analysis.md | Read through the AI's analysis and checked its claims against the code | Found that only AC-1 was actually tested, SR-17/18 aren't tied to a real flaw, and one branch in Tree 2 is already blocked by the code. Kept AC-7 and AC-11 after confirming they're real. |

