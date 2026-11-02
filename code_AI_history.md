# code_AI_history.md: AI Interaction Log for Source Code

**Project:** Resource Allocation & Help Desk Hub (CS763 PP Domain 2) · **Author:** Azmeer Hussain

## AI Usage Log (mandatory disclosure)

| Item | Value |
|---|---|
| Conceptual AI contribution | **95% AI / 5% human**. The agent wrote this log from the session, and the student reviewed it. |
| AI tool | Claude (Anthropic) agentic session, model `claude-opus-5-5` |
| Chat / session link | https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6 |
| Exported AI history | [`docs/ai-history/claude-session-export.md`](docs/ai-history/claude-session-export.md) (verbatim student prompts + agent replies) |

Exact prompts typed by the student (verbatim, typos kept):

1. "ive given you access to a folder that has a project in it for one of my classes, before youdo anything just read through it an get an understanding of what the project is asking. then give me a short understanding too cause its too long for m eto read"
2. "so lets set up the stuff that needs to be done for week 1? its dues 6am nov 3rd so i only have today to wkron it. what do you need from me?"
3. "i think we only do the first deiveryably and nthing extra. yes on the stack. go with the optioins that give me the least work and follow all acedemic integrity rules / ll open chrome so you have access just open all necesasry pages and ill log into them"
4. "im reading through the ai usage part, is it supposed to be 95% generated? in terms of the isntructions?"
5. "okay sweet so what am i supposed to do next? just make sure you go through the doc and follow all the urles to the t cause this professor is very anal about that"

(Status messages that contained no instructions, such as "okay the github is connecctd", are in the full export: `docs/ai-history/claude-session-export.md`.)

---

Each entry records one AI agent interaction that generated or modified code: what was asked, what was produced, what the human changed, and how the result was verified.
**Agent:** Claude (Anthropic), model `claude-opus-5-5`, Claude Code-based agentic session ([session link](https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6)). Full configuration is in `Agent_environment.md`.

---

## PA1-01 · 2026-11-02 · Read assignment
- **Prompt (student, verbatim):** "ive given you access to a folder that has a project in it for one of my classes, before youdo anything just read through it an get an understanding of what the project is asking…"
- **Agent output:** A chat summary of `CS763_ProjectDescription.docx`. No code.
- **Human modification:** None.

## PA1-02 · 2026-11-02 · spec.md
- **Prompt (student, verbatim):** #2 and #3 above ("so lets set up the stuff that needs to be done for week 1? …", "i think we only do the first deiveryably and nthing extra. yes on the stack. …")
- **Agent output:** `spec.md`: 27 REST endpoints, JSON Schemas, a Mermaid ER diagram, 6 tables, state machines and mock data structures.
- **Human modification:** The student chose the Domain 2 service and the Java 21 / Spring Boot / PostgreSQL stack, and reviewed the spec.
- **Verification:** Cross-checked against the Domain 2 text in the course description.

## PA1-03 · 2026-11-02 · Barebone Spring Boot application
- **Agent-internal task (written by the agent while executing student prompt #3; not typed by the student):** "Generate a barebone Spring Boot 3 / Java 21 application skeleton for the Resource Allocation & Help Desk Hub described in spec.md (PA1 only, no features). Add /api/v1/ping and tests."
- **Files:** `pom.xml`, `RahubApplication.java`, `web/PingController.java`, `application.yml`, `src/test/**`, `src/main/resources/mock-data/*.json`
- **Contribution:** 100% AI generated / 0% human refactored (the student set the scope but did not edit the code).
- **Verification:** `mvn -B verify` in GitHub Actions (`build-test` job). Two tests: context load, and ping returns `UP`.

## PA1-04 · 2026-11-02 · Docker
- **Agent-internal task (written by the agent while executing student prompt #3; not typed by the student):** "Write a multi-stage Dockerfile (Maven build stage → JRE runtime, non-root user, healthcheck) and a docker-compose.yml with the app and its own PostgreSQL 16."
- **Files:** `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- **Contribution:** 100% AI generated / 0% human refactored
- **Verification:** The `docker` job in CI runs `docker compose up`, waits for `/actuator/health` = UP, then calls `/api/v1/ping`.

## PA1-05 · 2026-11-02 · DevSecOps pipeline
- **Agent-internal task (written by the agent while executing student prompt #3; not typed by the student):** "Write .github/workflows/devsecops.yml for Java/Maven: build+test, docker build + smoke test, Trivy fs + image scan, Semgrep SAST, SonarQube Cloud SAST, all scans non-blocking, upload SARIF + reports as artifacts."
- **Files:** `.github/workflows/devsecops.yml`
- **Contribution:** 100% AI generated / 0% human refactored (the student created the SonarQube Cloud organization and authorised it on GitHub; no code edits)
- **Verification:** Green pipeline run on GitHub Actions. The log is saved in `docs/pipeline-logs/`.

## PA1-06 · 2026-11-02 · Pipeline logs
- **Agent action:** Exported the raw GitHub Actions logs of runs 41518893178 and 41520100815 and summarised them.
- **Files:** `docs/pipeline-logs/*`
- **Human action:** The student created the SonarQube Cloud organization and added the `SONAR_TOKEN` repository secret himself (the agent is not permitted to handle tokens).
- **Verification:** All 5 jobs ✅ in run 41520100815.

## PA1-07 · 2026-11-02 · Compliance review against the course rules
- **Prompt (student, verbatim):** #5 above ("…just make sure you go through the doc and follow all the urles to the t…")
- **Agent changes:** Quoted all prompts verbatim (they had been typo-corrected). Added an AI Usage Log to every markdown file. Relabelled agent-written task text in code headers so it is not presented as student prompts. Added headers to `.gitignore` / `.dockerignore`. Added a mock-data contribution note (JSON cannot hold comments). Exported the session history.
- **Contribution:** 100% AI (documentation fixes), reviewed by the student

## PA1-08 · 2026-11-02 · Documentation
- **Files:** `Agent_environment.md`, `Prompt_test_log.md`, `code_AI_history.md`, `README.md`
- **Contribution:** 90–95% AI / 5–10% human review
