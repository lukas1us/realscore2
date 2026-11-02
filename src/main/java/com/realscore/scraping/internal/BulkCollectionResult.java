package com.realscore.scraping.internal;

import java.util.Set;

/**
 * What one bulk collection run did.
 *
 * @param foundIds estate IDs the search returned, already known or not
 * @param alreadyKnown found IDs that already had a listing
 * @param fetched new IDs whose detail was fetched and recorded
 * @param refetched known IDs that were due and whose detail was fetched again
 * @param changed refetched IDs whose payload changed, so a new snapshot was written
 * @param priceChanged known IDs refetched before they were due because their search price changed; also counted in
 *                     {@code refetched}, or in {@code skipped} or {@code failed}
 * @param skipped new or due IDs Sreality answered 403, 404 or 410 for; nothing recorded
 * @param failed new or due IDs whose fetch failed in any other way; nothing recorded
 */
public record BulkCollectionResult(
        Set<Long> foundIds, int alreadyKnown, int fetched, int refetched, int changed, int priceChanged,
        int skipped, int failed) {

    public BulkCollectionResult {
        foundIds = Set.copyOf(foundIds);
    }

    /** How many estate IDs the search returned, each counted once. */
    public int found() {
        return foundIds.size();
    }

    /** Leaves out the IDs, which can run into thousands. */
    @Override
    public String toString() {
        return ("BulkCollectionResult[found=%d, alreadyKnown=%d, fetched=%d, refetched=%d, changed=%d, priceChanged=%d,"
                + " skipped=%d, failed=%d]")
                .formatted(found(), alreadyKnown, fetched, refetched, changed, priceChanged, skipped, failed);
    }
}
