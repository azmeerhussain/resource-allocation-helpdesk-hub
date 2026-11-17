# Manual verification evidence (student-run)

> **AI Usage Log:** The tests were run **by the student by hand** in a GitHub Codespace terminal. The commands were provided by the AI agent (Claude, `claude-opus-5-5`, see `../.ai_logs/pa2-session-log.md` PA2-06). The screenshots are the student's own captures. This README was written by the agent (100% AI).

| Date | File | What it shows |
|---|---|---|
| 2026-11-16 | [`2026-11-16_manual-negative-quantity_clean-run.png`](2026-11-16_manual-negative-quantity_clean-run.png) | Fresh database. Laptop **total 10 / available 10** → ticket for **quantity −5**, approved as `"Anyone"` and allocated → **total 10 / available 15**. Allocation logic does not bound quantity (stock is *increased*), and the approval needs no identity. |
| 2026-11-16 | [`2026-11-16_manual-negative-quantity_first-run.png`](2026-11-16_manual-negative-quantity_first-run.png) | First attempt, on a stack that already held one prior attack: available **20** with total 10 (two −5 attacks stacked). The response shows `"quantity":-5` and an issued `HW-` token. |

Environment: GitHub Codespaces (Ubuntu), `docker compose up` of `PA2/functional-codebase` (service + PostgreSQL 16), seeded mock data, requests via `curl`.
