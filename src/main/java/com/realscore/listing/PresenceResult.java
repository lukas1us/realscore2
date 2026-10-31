package com.realscore.listing;

/**
 * What {@link ListingSnapshotService#recordPresence} changed.
 *
 * @param reappeared seen listings that had been marked removed and no longer are
 * @param removed active listings that were not seen and are now marked removed
 */
public record PresenceResult(int reappeared, int removed) {
}
