#!/bin/sh
# ======================================================================================
# AI Contribution Weight: [100%] AI Generated / [0%] Human Refactored
# Tool: Claude (claude-opus-5-5). Student request: "lets go with both recommendations, and also
# future proof ourselves for further deliverables down the road" (PA2/.ai_logs, PA2-07)
# Human review: PENDING
# --------------------------------------------------------------------------------------
# Reusable Semgrep SAST scan, used by CI and by hand (PA2 triage, PA3 own + peer repo runs).
#
#   CI:     sh scripts/sast-scan.sh <target-dir> <out-dir>      (inside semgrep/semgrep image)
#   Local / Codespaces / peer repo (needs Docker), from the repo you want to scan:
#     docker run --rm -v "$PWD":/src -w /src semgrep/semgrep sh scripts/sast-scan.sh . reports
#   (for a peer repo, copy this script into it or pass its path)
#
# Outputs in <out-dir>: semgrep.json, semgrep.sarif, semgrep.txt, semgrep-summary.txt
# ======================================================================================
TARGET="${1:-.}"
OUT="${2:-reports}"
# Rulesets: language + secrets + container baseline, plus broad security audit / OWASP Top 10
# (broadened in PA2 so the triage has a real raw alert set, including false positives).
RULESETS="p/java p/secrets p/dockerfile p/security-audit p/owasp-top-ten"

mkdir -p "$OUT"
CONFIGS=""
for r in $RULESETS; do CONFIGS="$CONFIGS --config $r"; done

echo "Semgrep target: $TARGET | rulesets: $RULESETS"
# shellcheck disable=SC2086
semgrep scan $CONFIGS --metrics=off \
  --json-output "$OUT/semgrep.json" \
  --sarif-output "$OUT/semgrep.sarif" \
  --text-output "$OUT/semgrep.txt" \
  "$TARGET" || true

if [ ! -s "$OUT/semgrep.json" ]; then
  echo "ERROR: Semgrep produced no JSON output" >&2
  exit 1
fi

python3 - "$OUT/semgrep.json" "$OUT/semgrep-summary.txt" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
lines = []
for i, r in enumerate(data.get("results", []), 1):
    meta = r.get("extra", {}).get("metadata", {})
    cwe = meta.get("cwe", [])
    cwe = cwe[0] if isinstance(cwe, list) and cwe else cwe
    msg = " ".join(r.get("extra", {}).get("message", "").split())[:160]
    lines.append(f"F{i:02d} | {r.get('extra', {}).get('severity')} | {r['check_id']} | "
                 f"{r['path']}:{r['start']['line']} | {cwe} | {msg}")
summary = [f"SEMGREP FINDINGS: {len(lines)}"] + lines
errors = data.get("errors", [])
if errors:
    summary.append(f"SEMGREP ERRORS: {len(errors)} (see semgrep.json)")
open(sys.argv[2], "w").write("\n".join(summary) + "\n")
print("\n".join(summary))
PY
