package com.realscore.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.ListingSnapshotView;
import com.realscore.parsing.internal.ListingPricePoint;
import com.realscore.parsing.internal.ListingPricePointRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Snapshots are recorded through {@code recordFetch}, which parses each new one automatically (TASK-33), so these
 * tests also cover the history being kept up to date after a scrape. Each test uses its own source.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class ListingPriceHistoryTest {

    private static final String EXTERNAL_ID = "1";

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingDetailsReparseService reparseService;

    @Autowired
    private ListingPricePointRepository pricePointRepository;

    @Test
    void aNewListingStartsItsHistoryAtItsFirstSnapshot() {
        String source = "history-new";
        record(source, "3590000", "a");

        List<ListingSnapshotView> snapshots = listingSnapshotService.findSnapshots(source, EXTERNAL_ID);
        assertThat(prices(source)).containsExactly("3590000");
        assertThat(history(source).getFirst().getObservedAt()).isEqualTo(snapshots.getFirst().fetchedAt());
    }

    @Test
    void aChangedPayloadWithTheSamePriceAddsNoPoint() {
        String source = "history-same-price";
        record(source, "3590000", "a");
        record(source, "3590000", "b");

        assertThat(listingSnapshotService.findSnapshots(source, EXTERNAL_ID)).hasSize(2);
        assertThat(prices(source)).containsExactly("3590000");
    }

    @Test
    void eachPriceChangeAddsAPointAtTheSnapshotThatCarriedIt() {
        String source = "history-changes";
        record(source, "3590000", "a");
        record(source, "3590000", "b");
        record(source, "3390000", "c");
        record(source, "3590000", "d");

        List<ListingSnapshotView> snapshots = listingSnapshotService.findSnapshots(source, EXTERNAL_ID);
        assertThat(prices(source)).containsExactly("3590000", "3390000", "3590000");
        assertThat(history(source)).extracting(ListingPricePoint::getObservedAt).containsExactly(
                snapshots.get(0).fetchedAt(), snapshots.get(2).fetchedAt(), snapshots.get(3).fetchedAt());
    }

    @Test
    void snapshotsWithoutAPriceAreSkipped() {
        String source = "history-no-price";
        record(source, "0", "a");
        record(source, "3590000", "b");
        record(source, "0", "c");
        record(source, "3590000", "d");
        record(source, "3390000", "e");

        assertThat(prices(source)).containsExactly("3590000", "3390000");
    }

    @Test
    void reparseRebuildsTheHistoryAndASecondRunLeavesItIdentical() {
        String source = "history-reparse";
        record(source, "3590000", "a");
        record(source, "3390000", "b");
        clearHistory(source);

        ReparseResult first = reparseService.reparseAll(source);
        List<ListingPricePoint> afterFirstRun = history(source);
        ReparseResult second = reparseService.reparseAll(source);

        assertThat(first.failed()).isZero();
        assertThat(second.failed()).isZero();
        assertThat(prices(source)).containsExactly("3590000", "3390000");
        assertThat(history(source)).usingRecursiveFieldByFieldElementComparator().isEqualTo(afterFirstRun);
    }

    private void record(String source, String price, String marker) {
        listingSnapshotService.recordFetch(source, EXTERNAL_ID, """
                {"result": {"price_summary_czk": %s, "meta_description": "%s"}}
                """.formatted(price, marker));
    }

    private List<ListingPricePoint> history(String source) {
        return pricePointRepository.findByListingIdOrderByObservedAtAsc(listingId(source));
    }

    private List<String> prices(String source) {
        return history(source).stream().map(point -> point.getPriceCzk().toBigInteger().toString()).toList();
    }

    private void clearHistory(String source) {
        pricePointRepository.deleteAll(history(source));
    }

    private Long listingId(String source) {
        return listingSnapshotService.findLatestSnapshot(source, EXTERNAL_ID).orElseThrow().listingId();
    }
}
