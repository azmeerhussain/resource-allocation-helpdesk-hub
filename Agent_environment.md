# Agent_environment.md — Autonomous AI Developer Agent Configuration

**Project:** Resource Allocation & Help Desk Hub (CS763 PP Domain 2) · **Author:** Azmeer Hussain · **Phase:** PA1 · **Date:** 2026-11-02

---

## AI Usage Log (mandatory disclosure)

| Item | Value |
|---|---|
| Conceptual AI contribution | **90% AI drafted / 10% human**. The agent recorded its own configuration; the student chose the agent, reviewed the document and confirmed the details. |
| AI tool | Claude (Anthropic) agentic session, model `claude-opus-5-5` |
| Chat / session link | https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6 |
| Design-aid prompt | *"lets set up the stuff that needs to be done for week 1 … go with the options that give me the least work and follow all academic integrity rules"* |

---

## 1. Agent identity

| Field | Value |
|---|---|
| Agent | **Claude**: Anthropic's agentic assistant running on the **Claude Code** agent harness (Claude Agent SDK), used from the Claude app (claude.ai, "Claude Code on the web" remote session) |
| Vendor | Anthropic |
| LLM model | `claude-opus-5-5` (Claude Opus 5.5). The vendor's runtime can fall back to another model, and the configured identifier is what is recorded here. |
| Model knowledge cutoff | June 2026 |
| Session ID | `session_01VYTSL9tbQAUzzMVpp9wZR6` |

## 2. Operational mode

| Setting | Value |
|---|---|
| Mode | **Autonomous agent loop** (plan → tool call → observe → iterate) with a **human in the loop**. The student gives high-level goals in natural language, and the agent decides on and executes the steps (writing files, running shell commands, git, browser actions). |
| Permission mode | **Auto mode with a safety classifier.** Low-risk actions (file edits, shell commands inside the sandbox) run without per-step approval. High-impact actions are blocked by the classifier and handed back to the human. Examples: creating a public repository, accepting third-party terms, OAuth grants, entering credentials. In this session the student had to click "Create repository" on GitHub and authorise SonarQube Cloud himself. |
| Commit attribution | Agent-written commits are prefixed `AI AGENT BUILD: <feature>` and carry a `Co-Authored-By: Claude` trailer. Manual edits by the student are prefixed `HUMAN MOD: <feature>`. |

## 3. Tools available to the agent (tool use)

| Tool | Purpose in this project |
|---|---|
| `Bash` | Run shell commands in the sandbox: git, `mvn`, `docker`, file generation |
| `Read` / `Write` / `Edit` | Create and edit source files and markdown deliverables |
| `Glob` / `Grep` | Search the codebase |
| Remote-device file bridge (`device_list_dir`, `device_stage_files`) | Read the course project description from the student's Windows PC folder |
| Claude in Chrome (browser automation) | Open GitHub / SonarQube Cloud pages in the student's dedicated Chrome profile. The student logs in himself, and the agent never enters credentials. |
| GitHub integration | Push commits and tags to the repository |
| Sub-agents, web search | Available but not used in PA1 |

## 4. Hyperparameters and configuration

| Parameter | Value |
|---|---|
| Temperature / top-p / top-k | **Not user-configurable** in this product. Anthropic's defaults for the agent harness are used. |
| Reasoning effort | Product default (low/adaptive extended thinking) |
| Max output tokens / context window | Managed by the harness (not exposed). Long sessions are automatically compacted. |
| System prompt | Anthropic's built-in Claude Code / Claude app system prompt. No custom system prompt was added for PA1. A security-hardened "Secure-by-Design" prompt architecture comes in PA3. |
| Project memory files (`CLAUDE.md`) | None in PA1 |
| Prompting style (PA1) | Short natural-language task prompts from the student (logged verbatim in `Prompt_test_log.md` and `code_AI_history.md`) |

## 5. Host environment

### 5.1 Agent execution sandbox (where code is generated and run)

| Item | Value |
|---|---|
| Location | Isolated, ephemeral Linux container in Anthropic's cloud (one container per session) |
| OS / kernel | Ubuntu 24.04.5 LTS, Linux kernel 6.18.44 |
| CPU / RAM | 2 vCPU, ~7 GB RAM |
| Java | OpenJDK 21.0.12 |
| Maven | Apache Maven 3.9.11 |
| Docker | Docker CLI 29.9.2 |
| Git | Repository initialised and committed inside the sandbox, then pushed to GitHub |
| Network | Outbound traffic goes through an **egress allowlist proxy**. Maven Central and Docker Hub were **not reachable** from the sandbox, so the agent could not run `mvn package` / `docker build` locally. Compilation, tests, the Docker build and all scans are executed and verified in **GitHub Actions** instead (see `docs/pipeline-logs/`). |

### 5.2 Human operator workstation

| Item | Value |
|---|---|
| Machine | Student's Windows PC |
| Interface | Claude desktop app linked to the cloud session, plus a dedicated Google Chrome profile running the Claude in Chrome extension |
| Shared folder | `C:\Users\Azmee\Downloads\Important\FinalSem\securesoftware` (read access for the agent, holds the course project description) |

### 5.3 CI/CD execution environment

| Item | Value |
|---|---|
| Runner | GitHub-hosted `ubuntu-latest` |
| Pipeline | `.github/workflows/devsecops.yml` |
| Java in CI | Temurin 21 via `actions/setup-java@v4` |
| Scanners | Trivy `0.57.1` (filesystem/dependency + container image), Semgrep (`p/java`, `p/secrets`, `p/dockerfile`), SonarQube Cloud (Maven scanner) |

## 6. Reproducing this setup

1. Open a Claude Code session (claude.ai/code or the Claude desktop app) with model `claude-opus-5-5` and a GitHub account linked.
2. Attach this repository, then give the prompts listed in `Prompt_test_log.md`.
3. Add the repository secret `SONAR_TOKEN` (from SonarQube Cloud → My Account → Security) to enable the SonarQube job.
