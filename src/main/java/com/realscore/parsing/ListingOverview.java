package com.realscore.parsing;

import java.time.Instant;
import java.util.List;

/**
 * One listing as far as it is known: presence timestamps, parsed details ({@code null} if it was never parsed
 * successfully) and price history, oldest first. {@code outOfScopeSince} is set while the listing is online but no
 * longer returned by the searches (e.g. above the price cap).
 */
public record ListingOverview(
        String source,
        String externalId,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant removedAt,
        Instant outOfScopeSince,
        ListingDetails details,
        List<PricePoint> priceHistory) {
}
