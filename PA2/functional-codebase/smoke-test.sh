#!/usr/bin/env bash
# ======================================================================================
# Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
# Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
# Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
#                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
#                      PostgreSQL database" (student prompt PA2-01, verbatim in PA2/.ai_logs/pa2-session-log.md)
# AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
# Human Modifications: None
# Verification       : docker compose up + smoke-test.sh in CI (DevSecOps pipeline, job functional-docker)
#                      Human review: Azmeer Hussain, 2026-11-16 - reviewed, approves the ticket by just typing “IT Director” as the name, which shows anyone can approve
# Confidence         : Medium
# ======================================================================================
# End-to-end smoke test against the running docker compose stack (used by CI).
set -euo pipefail
BASE=${BASE_URL:-http://localhost:8080}

for i in $(seq 1 30); do
  if curl -fsS "$BASE/actuator/health" | grep -q '"status":"UP"'; then break; fi
  echo "waiting for app ($i)..."; sleep 5
done
curl -fsS "$BASE/actuator/health"; echo

echo "--- seeded assets (GET /api/v1/assets)"
curl -fsS "$BASE/api/v1/assets"; echo
LAPTOP=$(curl -fsS "$BASE/api/v1/assets?category=LAPTOP" | sed -E 's/.*"id":"([^"]+)".*/\1/' | head -c 36)
echo "laptop id: $LAPTOP"

echo "--- POST /api/v1/assets/tickets"
TICKET=$(curl -fsS -X POST "$BASE/api/v1/assets/tickets" -H 'Content-Type: application/json' \
  -d "{\"requesterName\":\"Smoke Test\",\"requesterRole\":\"INSTRUCTOR\",\"courseId\":\"CS763-F26\",\"items\":[{\"assetId\":\"$LAPTOP\",\"quantity\":1}]}" \
  | sed -E 's/^\{"id":"([^"]+)".*/\1/')
echo "ticket id: $TICKET"

echo "--- PUT approve"
curl -fsS -X PUT "$BASE/api/v1/assets/tickets/$TICKET" -H 'Content-Type: application/json' \
  -d '{"status":"APPROVED","reviewedBy":"IT Director"}'; echo
echo "--- POST allocate"
curl -fsS -X POST "$BASE/api/v1/assets/tickets/$TICKET/allocate" -H 'Content-Type: application/json' \
  -d '{"allocatedBy":"IT Admin"}'; echo
echo "--- GET allocations"
curl -fsS "$BASE/api/v1/assets/allocations?courseId=CS763-F26"; echo
echo "--- POST return"
curl -fsS -X POST "$BASE/api/v1/assets/$LAPTOP/return"; echo
echo "SMOKE TEST PASSED"
