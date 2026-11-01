package com.realscore.listing;

/**
 * Published by {@link ListingSnapshotService#recordFetch} when it wrote a new snapshot, i.e. the listing was
 * {@link RecordedFetch#CREATED created} or {@link RecordedFetch#CHANGED changed}. Listeners that need the snapshot to
 * be visible should run after the publishing transaction commits.
 */
public record ListingSnapshotRecorded(String source, String externalId) {
}
