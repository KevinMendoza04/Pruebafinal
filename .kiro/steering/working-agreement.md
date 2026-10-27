---
inclusion: always
---
# Working Agreement (how you must behave in every phase)

1. **Inspect before you change.** Check `git status`, current branch, remotes and existing files first. Preserve existing work. Never force-push, reset, rewrite history, or delete files without asking.
2. **Lightweight documentation (credits are limited).** Do NOT generate large spec documents (no requirements/design/tasks files, no EARS rewrites). Keep only two living files: `docs/assessment-checklist.md` (traceability table: ID | requirement | implementation location | verification | status) and `docs/design-notes.md` (sections "Assumptions" and "Architecture decisions"). Statuses: `NOT_STARTED | IN_PROGRESS | IMPLEMENTED | VERIFIED | BLOCKED`. IMPLEMENTED is not VERIFIED.
3. **Verify, don't claim.** Never say something works, a test passes, a container runs, or a push succeeded unless you ran it and saw the result. Paste the real command and a short real output excerpt in the phase report. If a tool is missing (Docker, network), say exactly what could not be verified and give the command for the owner to run.
4. **One phase at a time.** Do ONLY the phase of the current prompt. At the end: run the phase's acceptance commands, fix failures, commit, then STOP and report. Do not start the next phase.
5. **Phase report (in chat, short, Spanish for explanations):** what was built; files/classes created; commands run with real results; what is NOT verified; assumptions taken; risks.
6. **Defense note per phase.** Create `docs/defense/phase-N.md` (in Spanish) with: what was built and why; where it lives (exact package/class/method); how the flow executes step by step; 5 likely oral-defense questions with short answers; 2 short exercises for the owner. Use only classes and methods that really exist.
7. **Ask only when needed.** If something is not covered by the steering files, choose the simplest option satisfying the statement and record it under "Assumptions" in `docs/design-notes.md`. Ask the owner only if the choice is unsafe/irreversible or contradicts the official documents. Max one short batch of questions per phase.
8. **Do not invent.** No extra business features, no credit/affiliate logic, no gateway/config-server, no unneeded libraries.
9. **Security hygiene.** No real secrets in code, config, logs, Postman, docs or screenshots. Before every commit scan staged files for secrets and `.env`.
10. **Git.** Small meaningful commits in English (`feat:`, `test:`, `docs:`, `chore:`, `build:`), stage only intended files, push to the remote branch at the end of each phase if credentials allow, and confirm with `git log` / `git status`. If the push fails, report the exact error; do not claim success.
11. **Quality bar.** Code must compile, tests must be meaningful (no assertion-free tests), no commented-out junk, short comments explaining WHY on non-obvious business rules and on each fetch-strategy decision.
12. **Evidence is real.** Never fabricate screenshots, metrics, coverage or logs. If you cannot capture something, add it to `docs/evidence/CAPTURE-CHECKLIST.md` with exact steps for the owner.
