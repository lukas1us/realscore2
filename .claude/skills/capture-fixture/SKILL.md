---
name: capture-fixture
description: Fetch a real Sreality API response once and save it as a test fixture. The only allowed contact with the live API during development.
disable-model-invocation: true
---

# Capture a Sreality fixture

Arguments: `detail <estateId>` or `search <query params>`.

1. Endpoints and params are in the Backlog document "Sreality API". Use those, don't guess.
2. Make exactly ONE request with curl, sending the same User-Agent the client uses. No loops or pagination.
3. Save the pretty-printed JSON to `src/test/resources/fixtures/sreality/<detail|search>_<short_description>.json`.
   Name it by what it demonstrates (e.g. `detail_price_on_request.json`), not by its ID.
4. Remove personal data: broker names, phones, emails, and the seller's free-text contact details.
5. Compare the response structure to the "Sreality API" document. If fields differ (renamed, missing, new),
   report it and propose an update to the document. Don't edit the document silently.