# code_AI_history.md: AI Interaction Log for Source Code

**Project:** Resource Allocation & Help Desk Hub (CS763 PP Domain 2) · **Author:** Azmeer Hussain

Each entry records one AI agent interaction that generated or modified code: what was asked, what was produced, what the human changed, and how the result was verified.
**Agent:** Claude (Anthropic), model `claude-opus-5-5`, Claude Code-based agentic session ([session link](https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6)). Full configuration is in `Agent_environment.md`.

---

## PA1-01 · 2026-11-02 · Read assignment
- **Prompt (student):** "ive given you access to a folder that has a project in it for one of my classes, before youdo anything just read through it an get an understanding of what the project is asking…"
- **Agent output:** A chat summary of `CS763_ProjectDescription.docx`. No code.
- **Human modification:** None.

## PA1-02 · 2026-11-02 · spec.md
- **Prompt (student):** "so lets set up the stuff that needs to be done for week 1?" → "yes on the stack. go with the options that give me the least work and follow all academic integrity rules"
- **Agent output:** `spec.md`: 27 REST endpoints, JSON Schemas, a Mermaid ER diagram, 6 tables, state machines and mock data structures.
- **Human modification:** The student chose the Domain 2 service and the Java 21 / Spring Boot / PostgreSQL stack, and reviewed the spec.
- **Verification:** Cross-checked against the Domain 2 text in the course description.

## PA1-03 · 2026-11-02 · Barebone Spring Boot application
- **Agent sub-prompt:** "Generate a barebone Spring Boot 3 / Java 21 application skeleton for the Resource Allocation & Help Desk Hub described in spec.md (PA1 only, no features). Add /api/v1/ping and tests."
- **Files:** `pom.xml`, `RahubApplication.java`, `web/PingController.java`, `application.yml`, `src/test/**`, `src/main/resources/mock-data/*.json`
- **Contribution:** 95% AI / 5% human (the human set the scope: "first deliverable only, nothing extra").
- **Verification:** `mvn -B verify` in GitHub Actions (`build-test` job). Two tests: context load, and ping returns `UP`.

## PA1-04 · 2026-11-02 · Docker
- **Agent sub-prompt:** "Write a multi-stage Dockerfile (Maven build stage → JRE runtime, non-root user, healthcheck) and a docker-compose.yml with the app and its own PostgreSQL 16."
- **Files:** `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- **Contribution:** 95% AI / 5% human
- **Verification:** The `docker` job in CI runs `docker compose up`, waits for `/actuator/health` = UP, then calls `/api/v1/ping`.

## PA1-05 · 2026-11-02 · DevSecOps pipeline
- **Agent sub-prompt:** "Write .github/workflows/devsecops.yml for Java/Maven: build+test, docker build + smoke test, Trivy fs + image scan, Semgrep SAST, SonarQube Cloud SAST, all scans non-blocking, upload SARIF + reports as artifacts."
- **Files:** `.github/workflows/devsecops.yml`
- **Contribution:** 95% AI / 5% human (the human created the SonarQube Cloud organization and authorised it on GitHub)
- **Verification:** Green pipeline run on GitHub Actions. The log is saved in `docs/pipeline-logs/`.

## PA1-06 · 2026-11-02 · Documentation
- **Files:** `Agent_environment.md`, `Prompt_test_log.md`, `code_AI_history.md`, `README.md`
- **Contribution:** 90–95% AI / 5–10% human review
