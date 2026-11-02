# Verified Pipeline Execution Log (PA1)

> AI Usage Log: summary compiled by the Claude agent (`claude-opus-5-5`) from the raw GitHub Actions logs. 95% AI / 5% human review.

**Official PA1 run:** [DevSecOps Pipeline #41520100815](https://github.com/azmeerhussain/resource-allocation-helpdesk-hub/actions/runs/41520100815) · 2026-11-02 · result **✅ success**
Raw log: [`run-41520100815-full.txt`](run-41520100815-full.txt). The earlier run [#41518893178](https://github.com/azmeerhussain/resource-allocation-helpdesk-hub/actions/runs/41518893178) is kept as [`run-41518893178-full.txt`](run-41518893178-full.txt). It ran before `SONAR_TOKEN` was configured, so the SonarQube step was skipped.

| Job | Result | Key telemetry |
|---|---|---|
| Build & Unit Test (Maven) | ✅ | `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` |
| Docker Build, Smoke Test & Trivy | ✅ | Multi-stage image built. App and PostgreSQL containers `(healthy)`. `/actuator/health` → `{"status":"UP"}`. `/api/v1/ping` → `"status":"UP"` |
| Trivy: dependency/filesystem scan (`pom.xml`) | ✅ non-blocking | 4 HIGH (`org.postgresql:postgresql` 42.7.4, e.g. CVE-2025-49146) |
| Trivy: container image scan (`rahub:ci`) | ✅ non-blocking | OS layer (Alpine 3.24.3): 0. Java jar: 85 (8 CRITICAL, 33 HIGH, 30 MEDIUM, 14 LOW), e.g. CVE-2025-24813 in `tomcat-embed-core` 10.1.31 |
| Semgrep SAST (`p/java`, `p/secrets`, `p/dockerfile`) | ✅ non-blocking | `Ran 104 rules on 20 files: 0 findings` |
| SonarQube Cloud SAST | ✅ non-blocking | `ANALYSIS SUCCESSFUL`, [dashboard](https://sonarcloud.io/dashboard?id=azmeerhussain_resource-allocation-helpdesk-hub&branch=main) |

All scanners are configured with `--exit-code 0` / `|| true` / `continue-on-error`, so findings are reported but do not block the build, as PA1 requires. SARIF results are also uploaded to the repository's **Security → Code scanning** tab, and the full reports are attached to each run as artifacts (`trivy-reports`, `semgrep-reports`, `surefire-reports`).

The dependency/image vulnerabilities come from the AI agent choosing Spring Boot 3.3.5 (released October 2024). They are deliberately left unfixed in PA1 and serve as baseline telemetry for the triage in PA2.
