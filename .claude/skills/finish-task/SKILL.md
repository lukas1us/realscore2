---
name: finish-task
description: Finalise the current Backlog task after implementation. Use when the user says the task is done or asks to wrap up.
disable-model-invocation: true
---

# Finish a Backlog task

1. Run `backlog instructions task-finalization` if not read in this conversation.
2. Run `mvn verify`. If it is red, fix it and re-run. Never continue while it is red.
3. Check the diff (`git diff main...HEAD`) for:
    - changes outside the task's scope → list them and ask whether to revert
    - new dependencies in pom.xml → flag them
    - an edited existing Flyway migration → this is a hard error, revert it
    - one module reaching into another module's `internal/` package
4. Go through each acceptance criterion: name the test or file that proves it, then check it via the CLI.
5. Write implementation notes into the task: what changed, why, and any follow-ups (create new tasks for those, don't do them).
6. Give me a summary: what to review first, risky spots, and the proposed commit message.
7. Wait for my approval. Then commit according to the "Git" section in CLAUDE.md and show me `git log -1 --stat`.