package com.realscore.listing;

import java.time.Instant;

public record ListingSnapshotView(Long listingId, String source, String externalId, String payload, Instant fetchedAt) {
}
