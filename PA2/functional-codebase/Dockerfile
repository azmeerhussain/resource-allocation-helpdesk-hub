# ======================================================================================
# Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
# Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
# Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
#                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
#                      PostgreSQL database" (student prompt PA2-01, verbatim in PA2/.ai_logs/pa2-session-log.md)
# AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
# Human Modifications: None
# Verification       : docker compose up + smoke-test.sh in CI (DevSecOps pipeline, job functional-docker)
#                      Human review: Azmeer Hussain, 2026-11-16 - reviewed, no USER line so the container runs as root, unlike PA1
# Confidence         : Medium
# ======================================================================================
# ---------- Stage 1: build ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/target/helpdesk-hub-functional-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
