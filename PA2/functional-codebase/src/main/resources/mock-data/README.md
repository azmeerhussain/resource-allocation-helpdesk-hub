# Static mock data

> **AI Usage Log / header:** JSON cannot hold comments, so the header for `academic-units.json` and `seed-assets.json` is recorded here.
> Tool: Claude (`claude-opus-5-5`) · Prompt: student prompt PA2-01 (`PA2/.ai_logs/pa2-session-log.md`) · AI Contribution: 100% AI generated / 0% human refactored · Human Modifications: none · Verification: loaded at startup in the docker compose smoke test · Human review: Azmeer Hussain, 2026-11-16 - reviewed, says the keys are fake demo strings, nothing wrong with it · Confidence: High

`academic-units.json` stands in for the external campus directory (departments, courses, labs, IT directors), as the course isolation rule requires. `seed-assets.json` is demo inventory that is loaded when `HELPDESK_SEED=true`. All license keys are fake demo strings.
