-- TASK-40 (decision-8, amending decision-2): snapshots no longer keep the seller's contact data. Removes the broker
-- (result.user: name, email, phones) and the agency web URLs from every stored payload, and recomputes payload_hash
-- the way the app now computes it: SHA-256 of the payload's jsonb text form, so an unchanged listing is not
-- snapshotted again on the next run just because its stored payload was scrubbed.
UPDATE listing_snapshot s
SET payload = scrubbed.payload,
    payload_hash = encode(sha256(convert_to(scrubbed.payload::text, 'UTF8')), 'hex')
FROM (
    SELECT id, payload #- '{result,user}' #- '{result,premise,web_url}' #- '{result,premise,web_visible_url}' AS payload
    FROM listing_snapshot
) scrubbed
WHERE scrubbed.id = s.id;
