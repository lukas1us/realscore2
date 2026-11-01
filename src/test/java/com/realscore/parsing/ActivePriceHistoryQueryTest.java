package com.realscore.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The Postgres container is shared with other tests, so each test only looks at listings from its own source. */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class ActivePriceHistoryQueryTest {

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingDetailsQueryService queryService;

    @Test
    void returnsTheFullHistoryOfListingsWithAPricePointSinceTheCutoff() {
        String source = "history-query";
        Instant beforeFetches = Instant.now().minus(Duration.ofSeconds(1));
        record(source, "1", "3590000");
        record(source, "1", "3390000");

        List<ActivePriceHistory> histories = histories(beforeFetches, source);

        assertThat(histories).singleElement().satisfies(history -> {
            assertThat(history.externalId()).isEqualTo("1");
            assertThat(history.transactionTypeCode()).isEqualTo(1);
            assertThat(history.priceHistory()).extracting(point -> point.priceCzk().toBigInteger().toString())
                    .containsExactly("3590000", "3390000");
        });
    }

    @Test
    void leavesOutListingsWithoutAPricePointSinceTheCutoff() {
        String source = "history-query-old";
        record(source, "1", "3590000");

        assertThat(histories(Instant.now().plus(Duration.ofMinutes(1)), source)).isEmpty();
    }

    private List<ActivePriceHistory> histories(Instant since, String source) {
        return queryService.findActivePriceHistoriesChangedSince(since, null).stream()
                .filter(history -> history.source().equals(source))
                .toList();
    }

    private void record(String source, String externalId, String price) {
        listingSnapshotService.recordFetch(source, externalId, """
                {"result": {"price_summary_czk": %s, "category_type_cb": {"value": 1}}}
                """.formatted(price));
    }
}
