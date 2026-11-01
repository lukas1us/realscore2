package com.realscore.listing;

import java.time.Instant;

/** A listing's identity and presence timestamps; {@code removedAt} is {@code null} while the listing is active. */
public record ListingView(
        Long listingId,
        String source,
        String externalId,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant removedAt) {
}
