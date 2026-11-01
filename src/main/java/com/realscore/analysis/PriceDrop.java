package com.realscore.analysis;

import com.realscore.parsing.ActivePriceHistory;
import com.realscore.parsing.PricePoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * An active listing whose current price is below the price it had when a time window started; several drops inside
 * the window add up into one.
 *
 * @param oldPriceCzk the price in effect when the window started, or the first price if the listing appeared later
 * @param newPriceCzk the current price
 * @param dropPercent how much lower the new price is than the old one, in percent rounded to one decimal
 * @param droppedAt when the current price was first seen
 */
public record PriceDrop(
        String source,
        String externalId,
        TransactionType transactionType,
        String dispositionLabel,
        BigDecimal sizeM2,
        String city,
        String citypart,
        BigDecimal oldPriceCzk,
        BigDecimal newPriceCzk,
        BigDecimal dropPercent,
        Instant droppedAt) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** @return the drop of the listing over the window starting at {@code since}, or empty if its price didn't drop */
    public static Optional<PriceDrop> of(ActivePriceHistory listing, Instant since) {
        List<PricePoint> history = listing.priceHistory();
        if (history.isEmpty() || history.getLast().observedAt().isBefore(since)) {
            return Optional.empty();
        }
        PricePoint old = history.getFirst();
        for (PricePoint point : history) {
            if (point.observedAt().isBefore(since)) {
                old = point;
            }
        }
        PricePoint current = history.getLast();
        if (current.priceCzk().compareTo(old.priceCzk()) >= 0) {
            return Optional.empty();
        }
        BigDecimal percent = old.priceCzk().subtract(current.priceCzk()).multiply(HUNDRED)
                .divide(old.priceCzk(), 1, RoundingMode.HALF_UP);
        return Optional.of(new PriceDrop(listing.source(), listing.externalId(),
                TransactionType.ofCode(listing.transactionTypeCode()), listing.dispositionLabel(), listing.sizeM2(),
                listing.city(), listing.citypart(), old.priceCzk(), current.priceCzk(), percent, current.observedAt()));
    }
}
