package com.realscore.scraping.internal;

import java.util.Set;

/**
 * What one bulk collection run did.
 *
 * @param foundIds estate IDs the search returned, already known or not
 * @param alreadyKnown found IDs that already had a listing and were not fetched
 * @param fetched new IDs whose detail was fetched and recorded
 * @param skipped new IDs Sreality answered 403/404 for; nothing recorded
 * @param failed new IDs whose fetch failed in any other way; nothing recorded
 */
public record BulkCollectionResult(Set<Long> foundIds, int alreadyKnown, int fetched, int skipped, int failed) {

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
        return "BulkCollectionResult[found=%d, alreadyKnown=%d, fetched=%d, skipped=%d, failed=%d]"
                .formatted(found(), alreadyKnown, fetched, skipped, failed);
    }
}
