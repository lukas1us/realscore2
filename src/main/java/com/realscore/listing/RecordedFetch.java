package com.realscore.listing;

/**
 * What {@link ListingSnapshotService#recordFetch} did with a fetched payload.
 */
public enum RecordedFetch {
    /** First fetch of this listing: the listing and its first snapshot were created. */
    CREATED,
    /** Known listing whose payload changed: a new snapshot was written. */
    CHANGED,
    /** Known listing whose payload is the same as its latest snapshot: only its timestamps moved. */
    UNCHANGED
}
