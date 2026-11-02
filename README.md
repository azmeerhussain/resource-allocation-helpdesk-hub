# Resource Allocation & Help Desk Hub

CS763 Secure Software Development (BU MET), Project Portal **Domain 2**. A single-tenant Spring Boot service that allocates license keys, cloud credit vouchers and tracking parameters to student project teams, and runs a help desk for resource requests.

## AI Usage Log (mandatory disclosure)

| Item | Value |
|---|---|
| Conceptual AI contribution | Code: **100% AI generated / 0% human refactored**. Documents: **85–95% AI** (the student chose the domain and stack, set the scope, and reviewed everything) |
| AI tool | Claude (Anthropic) agentic session, model `claude-opus-5-5`. See `Agent_environment.md`. |
| Chat / session link | https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6 |
| AI history | [`code_AI_history.md`](code_AI_history.md), [`docs/ai-history/claude-session-export.md`](docs/ai-history/claude-session-export.md) |
| Commit labels | `AI AGENT BUILD: <feature>` (agent) / `HUMAN MOD: <feature>` (manual edits) |

Exact prompts typed by the student (verbatim, typos kept):

1. "ive given you access to a folder that has a project in it for one of my classes, before youdo anything just read through it an get an understanding of what the project is asking. then give me a short understanding too cause its too long for m eto read"
2. "so lets set up the stuff that needs to be done for week 1? its dues 6am nov 3rd so i only have today to wkron it. what do you need from me?"
3. "i think we only do the first deiveryably and nthing extra. yes on the stack. go with the optioins that give me the least work and follow all acedemic integrity rules / ll open chrome so you have access just open all necesasry pages and ill log into them"
4. "im reading through the ai usage part, is it supposed to be 95% generated? in terms of the isntructions?"
5. "okay sweet so what am i supposed to do next? just make sure you go through the doc and follow all the urles to the t cause this professor is very anal about that"

(Status messages that contained no instructions, such as "okay the github is connecctd", are in the full export: `docs/ai-history/claude-session-export.md`.)


## PA1 deliverables

| File | Purpose |
|---|---|
| [`spec.md`](spec.md) | Service specification: endpoints, schemas, data model, mock data |
| [`Agent_environment.md`](Agent_environment.md) | AI developer agent configuration |
| [`Prompt_test_log.md`](Prompt_test_log.md) | Initial prompts and generated code |
| [`.github/workflows/devsecops.yml`](.github/workflows/devsecops.yml) | CI/CD DevSecOps pipeline (Build → Docker → Trivy → Semgrep → SonarQube) |
| [`Dockerfile`](Dockerfile), [`docker-compose.yml`](docker-compose.yml) | Multi-stage image and the app + PostgreSQL stack |
| [`docs/pipeline-logs/`](docs/pipeline-logs/README.md) | Verified pipeline execution log (summary + raw logs) |
| [`code_AI_history.md`](code_AI_history.md) | AI interaction history |
| [`docs/ai-history/`](docs/ai-history/claude-session-export.md) | Exported AI session history |

## Run locally

```bash
docker compose up --build
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/ping
```
