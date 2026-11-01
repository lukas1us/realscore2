package com.realscore.analysis;

import com.realscore.parsing.ListingOverview;
import com.realscore.parsing.PricePoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Figures derived from a listing's presence and price history.
 *
 * @param daysOnMarket whole days from when realscore first saw the listing to its removal, or to now if active
 * @param priceDrops how many price history points are lower than the one before
 * @param priceChangePercent change from the first to the current price, in percent rounded to one decimal;
 *     {@code null} without a price history
 */
public record ListingStats(long daysOnMarket, int priceDrops, BigDecimal priceChangePercent) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static ListingStats of(ListingOverview listing, Instant now) {
        Instant end = listing.removedAt() != null ? listing.removedAt() : now;
        long days = Math.max(0, Duration.between(listing.firstSeenAt(), end).toDays());
        List<PricePoint> history = listing.priceHistory();
        return new ListingStats(days, priceDrops(history), priceChangePercent(history));
    }

    private static int priceDrops(List<PricePoint> history) {
        int drops = 0;
        for (int i = 1; i < history.size(); i++) {
            if (history.get(i).priceCzk().compareTo(history.get(i - 1).priceCzk()) < 0) {
                drops++;
            }
        }
        return drops;
    }

    private static BigDecimal priceChangePercent(List<PricePoint> history) {
        if (history.isEmpty()) {
            return null;
        }
        BigDecimal first = history.getFirst().priceCzk();
        BigDecimal current = history.getLast().priceCzk();
        return current.subtract(first).multiply(HUNDRED).divide(first, 1, RoundingMode.HALF_UP);
    }
}
