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
5. Check `README.md` against the changes. If they add, remove or change an HTTP endpoint, a configuration
   property or env var (or its default), a run/test command, a requirement, or behaviour the README describes,
   update `README.md` now and add it to the files to commit. Verify every value you write against the code and
   `application.properties`. If nothing user-facing changed, say so in the summary.
6. Go through each acceptance criterion: name the test or file that proves it, then check it via the CLI.
7. Write implementation notes into the task: what changed, why, and any follow-ups
   (create new Backlog tasks for those, don't do them). Set the status to Done via the CLI.
8. Give me a summary:
    - what to review first and the risky spots
    - the list of files that will be committed
    - the proposed commit message, following the "Git" section in CLAUDE.md
9. Wait for my approval. Then stage exactly the listed files (code + Backlog task files),
   commit, and show me `git log -1 --stat`. Never push.