package com.realscore.analysis;

import com.realscore.parsing.ListingDetails;
import com.realscore.parsing.ListingOverview;
import com.realscore.parsing.PricePoint;
import java.time.Instant;
import java.util.List;

/**
 * One listing with its parsed details ({@code null} if never parsed), price history (oldest first), stats, and score
 * ({@code null} unless it is an active, scorable flat for sale).
 */
public record ListingResponse(
        String source,
        String externalId,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant removedAt,
        ListingDetails details,
        List<PricePoint> priceHistory,
        ListingStats stats,
        ListingScore score) {

    static ListingResponse of(ListingOverview listing, ListingStats stats, ListingScore score) {
        return new ListingResponse(listing.source(), listing.externalId(), listing.firstSeenAt(),
                listing.lastSeenAt(), listing.removedAt(), listing.details(), listing.priceHistory(), stats, score);
    }
}
