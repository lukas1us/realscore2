package com.realscore.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.internal.Listing;
import com.realscore.listing.internal.ListingRepository;
import com.realscore.listing.internal.ListingSnapshotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class ListingSnapshotServiceTest {

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingSnapshotRepository listingSnapshotRepository;

    @Test
    void savingTheSamePayloadTwiceWritesExactlyOneSnapshot() {
        listingSnapshotService.recordFetch("sreality", "1", "{\"price\":100}");
        listingSnapshotService.recordFetch("sreality", "1", "{\"price\":100}");

        Listing listing = listingRepository.findBySourceAndExternalId("sreality", "1").orElseThrow();
        assertThat(listingSnapshotRepository.countByListing(listing)).isEqualTo(1);
    }

    @Test
    void savingAChangedPayloadWritesASecondSnapshot() {
        listingSnapshotService.recordFetch("sreality", "2", "{\"price\":100}");
        listingSnapshotService.recordFetch("sreality", "2", "{\"price\":200}");

        Listing listing = listingRepository.findBySourceAndExternalId("sreality", "2").orElseThrow();
        assertThat(listingSnapshotRepository.countByListing(listing)).isEqualTo(2);
    }

    @Test
    void findKnownExternalIdsReturnsOnlyIdsWithAListingFromThatSource() {
        listingSnapshotService.recordFetch("sreality", "3", "{\"price\":100}");
        listingSnapshotService.recordFetch("other", "4", "{\"price\":100}");

        assertThat(listingSnapshotService.findKnownExternalIds("sreality", List.of("3", "4", "5")))
                .containsExactly("3");
    }

    @Test
    void findKnownExternalIdsHandlesMoreIdsThanOneQueryBatch() {
        listingSnapshotService.recordFetch("sreality", "6", "{\"price\":100}");
        List<String> ids = new ArrayList<>(IntStream.range(10_000, 12_500).mapToObj(String::valueOf).toList());
        ids.add("6");

        assertThat(listingSnapshotService.findKnownExternalIds("sreality", ids)).containsExactly("6");
    }

    @Test
    void recordPresenceMarksUnseenListingsRemovedAndClearsReappearedOnes() {
        String source = "presence-test";
        listingSnapshotService.recordFetch(source, "a", "{}");
        listingSnapshotService.recordFetch(source, "b", "{}");
        listingSnapshotService.recordFetch("other", "b", "{}");

        PresenceResult firstRun = listingSnapshotService.recordPresence(source, List.of("a"), Instant.now());

        assertThat(firstRun).isEqualTo(new PresenceResult(0, 1));
        assertThat(removedAt(source, "a")).isNull();
        assertThat(removedAt(source, "b")).isNotNull();
        assertThat(removedAt("other", "b")).isNull();

        PresenceResult secondRun = listingSnapshotService.recordPresence(source, List.of("a", "b"), Instant.now());

        assertThat(secondRun).isEqualTo(new PresenceResult(1, 0));
        assertThat(removedAt(source, "b")).isNull();
    }

    @Test
    void recordFetchReportsWhetherTheListingWasCreatedChangedOrUnchanged() {
        String source = "recorded-fetch-test";

        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"price\":100}")).isEqualTo(RecordedFetch.CREATED);
        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"price\":100}")).isEqualTo(RecordedFetch.UNCHANGED);
        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"price\":200}")).isEqualTo(RecordedFetch.CHANGED);
    }

    @Test
    void unchangedFetchStillMovesLastFetchedAt() throws InterruptedException {
        String source = "last-fetched-test";
        listingSnapshotService.recordFetch(source, "1", "{}");
        Instant firstFetch = listingRepository.findBySourceAndExternalId(source, "1").orElseThrow().getLastFetchedAt();
        Thread.sleep(5);

        listingSnapshotService.recordFetch(source, "1", "{}");

        assertThat(listingRepository.findBySourceAndExternalId(source, "1").orElseThrow().getLastFetchedAt())
                .isAfter(firstFetch);
    }

    @Test
    void findExternalIdsFetchedBeforeReturnsOnlyListingsLastFetchedBeforeTheCutoff() {
        String source = "fetched-before-test";
        listingSnapshotService.recordFetch(source, "1", "{}");
        Instant afterFetch = Instant.now().plusSeconds(1);

        assertThat(listingSnapshotService.findExternalIdsFetchedBefore(source, List.of("1", "2"), afterFetch))
                .containsExactly("1");
        assertThat(listingSnapshotService.findExternalIdsFetchedBefore(source, List.of("1", "2"), Instant.EPOCH))
                .isEmpty();
    }

    private Instant removedAt(String source, String externalId) {
        return listingRepository.findBySourceAndExternalId(source, externalId).orElseThrow().getRemovedAt();
    }
}
