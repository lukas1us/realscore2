---
name: explain-diff
description: Explain recent code changes so the user understands them. Use when the user asks "explain", "walk me through" or "why is it like this".
---

# Explain the diff

Goal: I must understand every line well enough to maintain it without you.

1. Take `git diff` (uncommitted) or `git diff main...HEAD` if the working tree is clean.
2. Explain in this order:
    - Flow first: what happens at runtime, step by step, with class.method names.
    - Then each file: its responsibility and why it lives in that module.
    - Then non-obvious parts: Spring/JPA magic (transactions, proxies, lazy loading, @Scheduled threading),
      retry and rate-limit logic, SQL/JSONB details.
3. For every design choice name one alternative and why it wasn't used.
4. End with 3 questions I should be able to answer about this code. Don't give the answers unless I ask.
   Do not change any code.