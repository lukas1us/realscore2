---
name: new-migration
description: Create a new Flyway migration. Use whenever the DB schema needs to change.
---

# New Flyway migration

1. List `src/main/resources/db/migration/`. Take the next version number. Never reuse or edit an existing file.
2. Create `V<n>__<snake_case_description>.sql`. Use plain PostgreSQL 16, explicit constraint names, and `TIMESTAMPTZ` for timestamps.
3. Update the matching JPA entities. `ddl-auto=validate` must pass.
4. Run the Testcontainers test suite to prove the migration applies from scratch.
5. Show me the SQL separately before anything else. I review schema changes line by line.