---
name: finish-task
description: Finalise the current Backlog task after implementation. Use when the user says the task is done or asks to wrap up.
disable-model-invocation: true
---

# Finish a Backlog task

1. Run `backlog instructions task-finalization` if not read in this conversation.
2. Run `mvn verify`. If it is red, fix it and re-run. Never continue while it is red.
3. Collect the changes:
    - `git status --porcelain`: modified AND untracked files
    - `git diff` for modified files, and read untracked files in full
      (`git diff` doesn't show them)
    - if on a task branch (not `main`), also include `git diff main...HEAD`
4. Check the changes for:
    - files outside the task's scope → list them and ask whether to revert
    - new dependencies in pom.xml → flag them
    - an edited existing Flyway migration → this is a hard error, revert it
    - one module reaching into another module's `internal/` package
    - `.env`, IDE files or build output among the changes → never commit these
5. Go through each acceptance criterion: name the test or file that proves it, then check it via the CLI.
6. Write implementation notes into the task: what changed, why, and any follow-ups
   (create new Backlog tasks for those, don't do them). Set the status to Done via the CLI.
7. Give me a summary:
    - what to review first and the risky spots
    - the list of files that will be committed
    - the proposed commit message, following the "Git" section in CLAUDE.md
8. Wait for my approval. Then stage exactly the listed files (code + Backlog task files),
   commit, and show me `git log -1 --stat`. Never push.