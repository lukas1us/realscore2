---
name: start-task
description: Start work on a Backlog task. Use when the user says "start", "work on" or "pick up" a task ID.
disable-model-invocation: true
---

# Start a Backlog task

Argument: task ID (e.g. TASK-12). If none was given, run `backlog task list --plain -s "To Do"` and ask which one.

1. Run `backlog instructions task-execution` if you haven't read it in this conversation.
2. `backlog task <id> --plain`. Read the description, acceptance criteria and dependencies.
   If any dependency is not Done, stop and tell me.
3. Read every Backlog document or decision the task references (e.g. "Sreality API").
4. Read the code the task will touch. Note which module it belongs to (scraping / listing / parsing / analysis).
5. Present a plan:
    - files to create or change, grouped by module
    - any new public types a module exposes to other modules
    - tests that will prove each acceptance criterion
    - anything in the task that is unclear or that conflicts with CLAUDE.md
6. STOP and wait for approval. Do not write code yet.
7. After approval: record the plan in the task via the CLI, set status to In Progress, then implement.