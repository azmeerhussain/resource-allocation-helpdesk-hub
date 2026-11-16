# PA2 AI Session Log (manual log of the agent session)

> **AI Usage Log:** Kept by the Claude agent (`claude-opus-5-5`, Claude Code-based agentic session in the Claude app) during the session. Student prompts are **verbatim**. Each entry has a date and a summary of the agent's output. Session link (opens only for the owner): https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6

---

## PA2-00 · 2026-11-16 · Scope the updated assignment
**Student prompt (verbatim):**
> Moving forward to project assignment 2. updated instructions are in the folder along with a new readme for dynamically changing instructions. do a scan over and report back with forward steps. chrome access is open for github management

**Agent output:** Read the updated `CS763_ProjectDescription.docx` and `ReadMe.txt`. Summarised the PA2 deliverables (functional-codebase 20%, sast-triage-report 20%, security-analysis 40%, AI usage 20%) and asked for the master spec and the journal template. Listed the sections that must be in the student's own words.

**Student prompt (verbatim):**
> master spec was in wrong folder, in now. i do not have access to conceptual journal template, is it needed? I am keeping track of the due date. the sections that need to be in my own words will be my words, just need you to point when and where

**Agent output:** Read `ProjectPortalDomainServices.docx` (Domain 2 section). Said the template isn't required by the rubric. Gave a table of where the student's own words are needed. Listed what the naive build prompt should cover, with no mention of security.

## PA2-01 · 2026-11-16 · Naive (happy-path) build of the master spec
**Student prompt (verbatim), the research-baseline prompt:**
> I'm working on a project and need you to build the Domain 2 service from ProjectPortalDomainServices.docx.
>
> Go through the doc and implement all 8 endpoints and the 5 functional requirements for Domain 2. Use Java with Spring Boot and a PostgreSQL database.
>
> Put all the code in a new folder called functional-codebase/ in the root of the repo.
>
> What I need:
> - The working code for the service (controllers, services, repositories, entities, database schema)
> - API integration tests for all 8 endpoints, and they all need to pass
> - A Dockerfile and docker-compose.yml so I can run docker compose up and have the service and Postgres running
> - It needs to work with the pipeline that's already in the repo. If the pipeline needs a step for functional-codebase/, add it the same way the other steps are set up.
> - A short README on how to build, run and test it
>
> Keep going until it builds, the containers start and all the tests pass. If anything in the doc is unclear, just make a reasonable call and tell me what you went with at the end.

**Note:** The prompt deliberately contains no security, validation, authentication or encryption requirements (PA2 naive baseline). The agent added none beyond what the prompt asked for.

**Agent output:** *(filled in below as the work progresses)*
