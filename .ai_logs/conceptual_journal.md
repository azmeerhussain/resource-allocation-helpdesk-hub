# Conceptual Journal: PA2 (Resource Allocation & Help Desk Hub)

> **AI Deliverable Metadata Block**
> | Item | Value |
> |---|---|
> | Author | Azmeer Hussain |
> | Conceptual AI contribution | **~0% of the journal text.** All entries are written by the student. The AI agent (Claude, `claude-opus-5-5`) only created this file, added the headings and pasted the student's entries in verbatim. |
> | AI involvement in what is reflected on | The code being reviewed was 100% AI-generated (PA2-01). For entry 1, the agent pointed the student to the files and line ranges worth reviewing (`.ai_logs/pa2-session-log.md`, PA2-02); the review and the opinions are the student's. |
> | Session link | https://claude.ai/code/session_01VYTSL9tbQAUzzMVpp9wZR6 (owner access) |

---

## Entry 1 · 2026-11-16 · Code review of the AI-generated functional codebase

Overall the AI built what I asked for. The endpoints work, the tests pass and Docker runs. But I never mentioned security, so it didn’t add any, and that shows up all over the code.

**TicketService:** This had the biggest problems. If two people request the same asset at the same time, the stock can go below zero. Every allocation gets the same license key, and the key gets printed in the logs. When a ticket is rejected, the reserved assets never get released.

**AssetService:** You can set the stock or status to anything, even negative numbers. You can also delete an asset that someone is still using.

**AllocationService:** Anyone can see every license key just by calling one endpoint.

**Controllers:** There’s no login or role check, so anyone can approve tickets or hand out assets.

**Database:** There are no rules to stop bad data, like negative stock or missing fields.

**Other stuff:** The passwords are hardcoded, the error messages show too much, and the container runs as root, which my PA1 version didn’t.

**Tests:** They only check that things work when used normally. Nothing tests what happens when someone misuses the API.

**Do I agree with it?** Partly. The code is clean and easy to follow, but a lot of these issues are things a good developer would catch without being told. The AI focused on getting it working and passing tests, not on making it safe.
