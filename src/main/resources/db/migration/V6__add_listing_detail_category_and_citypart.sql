-- Lets analysis tell sales from rentals and flats from other property types, and group large cities by city part
-- (TASK-22.2). Codes are Sreality's raw category_type_cb / category_main_cb values (doc "Sreality API"):
-- transaction_type_code 1 = prodej, 2 = pronájem; property_category_code 1 = byty.
-- Existing rows stay NULL until the snapshots are re-parsed (POST /api/parsing/reparse).
ALTER TABLE listing_detail
    ADD COLUMN transaction_type_code INTEGER,
    ADD COLUMN property_category_code INTEGER,
    ADD COLUMN citypart VARCHAR(255);
