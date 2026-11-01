---
id: TASK-40
title: Strip seller PII before hashing and storing snapshots
status: To Do
assignee: []
created_date: '2026-11-01T17:32:00Z'
labels:
  - scraping
  - listing
milestone: m-7
dependencies: []
type: bug
ordinal: 50000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Raw snapshots store the seller's contact data (`result.user` holding user_email and user_phones, plus `premise.web_url` and `premise.web_visible_url`). It's personal data the app does not need. decision-2 says payloads are stored as-is, so the change needs a recorded decision.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 result.user, premise.web_url and premise.web_visible_url are removed from the payload before it is hashed and inserted
- [ ] #2 A new Flyway migration scrubs existing listing_snapshot rows
- [ ] #3 A decision amending decision-2 is recorded
- [ ] #4 No listing_snapshot.raw contains user_email or user_phones (test)
- [ ] #5 After the migration, the next run does not create a new snapshot for an unchanged listing only because PII was stripped
<!-- AC:END -->
