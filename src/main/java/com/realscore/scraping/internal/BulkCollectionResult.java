package com.realscore.scraping.internal;

/**
 * What one bulk collection run did.
 *
 * @param found estate IDs the search returned, each counted once
 * @param alreadyKnown found IDs that already had a listing and were not fetched
 * @param fetched new IDs whose detail was fetched and recorded
 * @param skipped new IDs Sreality answered 403/404 for; nothing recorded
 * @param failed new IDs whose fetch failed in any other way; nothing recorded
 */
public record BulkCollectionResult(int found, int alreadyKnown, int fetched, int skipped, int failed) {
}
