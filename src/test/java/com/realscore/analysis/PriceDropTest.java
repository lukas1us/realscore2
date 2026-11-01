package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.parsing.ActivePriceHistory;
import com.realscore.parsing.PricePoint;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceDropTest {

    private static final Instant SINCE = Instant.parse("2026-10-01T00:00:00Z");

    @Test
    void aSingleDropInsideTheWindowComparesToThePriceBeforeIt() {
        PriceDrop drop = PriceDrop.of(listing(point(-10, "3590000"), point(5, "3390000")), SINCE).orElseThrow();

        assertThat(drop.oldPriceCzk()).isEqualByComparingTo("3590000");
        assertThat(drop.newPriceCzk()).isEqualByComparingTo("3390000");
        assertThat(drop.dropPercent()).isEqualByComparingTo("5.6");
        assertThat(drop.droppedAt()).isEqualTo(SINCE.plus(Duration.ofDays(5)));
        assertThat(drop.transactionType()).isEqualTo(TransactionType.SALE);
    }

    @Test
    void severalDropsInsideTheWindowAddUp() {
        PriceDrop drop = PriceDrop.of(
                listing(point(-10, "4000000"), point(2, "3800000"), point(5, "3600000")), SINCE).orElseThrow();

        assertThat(drop.oldPriceCzk()).isEqualByComparingTo("4000000");
        assertThat(drop.newPriceCzk()).isEqualByComparingTo("3600000");
        assertThat(drop.dropPercent()).isEqualByComparingTo("10.0");
        assertThat(drop.droppedAt()).isEqualTo(SINCE.plus(Duration.ofDays(5)));
    }

    @Test
    void aDropPartlyReversedStillCountsFromTheStartOfTheWindow() {
        PriceDrop drop = PriceDrop.of(
                listing(point(-10, "4000000"), point(2, "3600000"), point(5, "3800000")), SINCE).orElseThrow();

        assertThat(drop.newPriceCzk()).isEqualByComparingTo("3800000");
        assertThat(drop.dropPercent()).isEqualByComparingTo("5.0");
    }

    @Test
    void aDropFullyReversedIsNoDrop() {
        assertThat(PriceDrop.of(listing(point(-10, "4000000"), point(2, "3600000"), point(5, "4000000")), SINCE))
                .isEmpty();
    }

    @Test
    void aRiseIsNoDrop() {
        assertThat(PriceDrop.of(listing(point(-10, "3600000"), point(2, "3800000")), SINCE)).isEmpty();
    }

    @Test
    void aDropBeforeTheWindowIsNoDrop() {
        assertThat(PriceDrop.of(listing(point(-10, "4000000"), point(-5, "3600000")), SINCE)).isEmpty();
    }

    @Test
    void aListingThatAppearedInsideTheWindowComparesToItsFirstPrice() {
        PriceDrop drop = PriceDrop.of(listing(point(1, "20000"), point(3, "18000")), SINCE).orElseThrow();

        assertThat(drop.oldPriceCzk()).isEqualByComparingTo("20000");
        assertThat(drop.dropPercent()).isEqualByComparingTo("10.0");
    }

    @Test
    void aSinglePriceIsNoDrop() {
        assertThat(PriceDrop.of(listing(point(1, "20000")), SINCE)).isEmpty();
    }

    private static ActivePriceHistory listing(PricePoint... history) {
        return new ActivePriceHistory("test", "1", 1, "2+kk", new BigDecimal("54"), "Plzeň", null, List.of(history));
    }

    private static PricePoint point(int daysFromSince, String price) {
        return new PricePoint(SINCE.plus(Duration.ofDays(daysFromSince)), new BigDecimal(price));
    }
}
