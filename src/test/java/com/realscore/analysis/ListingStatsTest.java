package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.parsing.ListingOverview;
import com.realscore.parsing.PricePoint;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ListingStatsTest {

    private static final Instant FIRST_SEEN = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-11T09:00:00Z");

    @Test
    void withoutPriceHistoryThereAreNoDropsAndNoChange() {
        ListingStats stats = ListingStats.of(listing(null, List.of()), NOW);

        assertThat(stats.priceDrops()).isZero();
        assertThat(stats.priceChangePercent()).isNull();
    }

    @Test
    void aSinglePriceIsNoChange() {
        ListingStats stats = ListingStats.of(listing(null, prices("3590000")), NOW);

        assertThat(stats.priceDrops()).isZero();
        assertThat(stats.priceChangePercent()).isEqualByComparingTo("0");
    }

    @Test
    void countsEachDropAndComparesTheCurrentToTheFirstPrice() {
        ListingStats stats = ListingStats.of(listing(null, prices("3590000", "3390000", "3490000", "3290000")), NOW);

        assertThat(stats.priceDrops()).isEqualTo(2);
        assertThat(stats.priceChangePercent()).isEqualByComparingTo("-8.4");
    }

    @Test
    void aRiseIsAPositiveChange() {
        ListingStats stats = ListingStats.of(listing(null, prices("20000", "21000")), NOW);

        assertThat(stats.priceDrops()).isZero();
        assertThat(stats.priceChangePercent()).isEqualByComparingTo("5.0");
    }

    @Test
    void anActiveListingCountsWholeDaysUntilNow() {
        assertThat(ListingStats.of(listing(null, List.of()), NOW).daysOnMarket()).isEqualTo(9);
    }

    @Test
    void aRemovedListingCountsDaysUntilItsRemoval() {
        Instant removedAt = FIRST_SEEN.plus(Duration.ofDays(3));

        assertThat(ListingStats.of(listing(removedAt, List.of()), NOW).daysOnMarket()).isEqualTo(3);
    }

    private static ListingOverview listing(Instant removedAt, List<PricePoint> history) {
        return new ListingOverview("test", "1", FIRST_SEEN, NOW, removedAt, null, null, history);
    }

    private static List<PricePoint> prices(String... prices) {
        return IntStream.range(0, prices.length)
                .mapToObj(i -> new PricePoint(FIRST_SEEN.plus(Duration.ofDays(i)), new BigDecimal(prices[i])))
                .toList();
    }
}
