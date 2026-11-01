-- TASK-37: building condition (Sreality building_condition, e.g. Novostavba, Po rekonstrukci) is a strong value
-- signal that quality scoring (TASK-35) needs. The code is Sreality's raw building_condition value and the label its
-- name; a missing or not-entered (value 0) condition is NULL. Existing rows stay NULL until the snapshots are
-- re-parsed (POST /api/parsing/reparse?source=sreality).
ALTER TABLE listing_detail
    ADD COLUMN building_condition_code INTEGER,
    ADD COLUMN building_condition_label VARCHAR(255);
