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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest
@Import(PostgresContainerConfig.class)
@RecordApplicationEvents
class ListingSnapshotServiceTest {

    @Autowired
    private ApplicationEvents events;

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
    void recordPresenceMarksSeenListingsAndClearsReappearedOnesButRemovesNothing() {
        String source = "presence-test";
        listingSnapshotService.recordFetch(source, "a", "{}");
        listingSnapshotService.recordFetch(source, "b", "{}");
        listingSnapshotService.markRemoved(source, List.of("b"), Instant.now());

        assertThat(listingSnapshotService.recordPresence(source, List.of("a"))).isEqualTo(0);
        assertThat(removedAt(source, "a")).isNull();
        assertThat(removedAt(source, "b")).isNotNull();

        assertThat(listingSnapshotService.recordPresence(source, List.of("a", "b"))).isEqualTo(1);
        assertThat(removedAt(source, "b")).isNull();
    }

    @Test
    void findActiveExternalIdsNotSeenSinceLeavesOutSeenAndRemovedListingsAndOtherSources() {
        String source = "unseen-test";
        listingSnapshotService.recordFetch(source, "old", "{}");
        listingSnapshotService.recordFetch(source, "old-removed", "{}");
        listingSnapshotService.recordFetch("unseen-other-source", "old", "{}");
        listingSnapshotService.markRemoved(source, List.of("old-removed"), Instant.now());
        Instant runStartedAt = Instant.now().plusMillis(1);
        listingSnapshotService.recordFetch(source, "seen", "{}");
        listingSnapshotService.recordPresence(source, List.of("seen"));

        assertThat(listingSnapshotService.findActiveExternalIdsNotSeenSince(source, runStartedAt))
                .containsExactly("old");
    }

    @Test
    void markRemovedMarksOnlyTheGivenActiveListingsOfTheSource() {
        String source = "mark-removed-test";
        listingSnapshotService.recordFetch(source, "1", "{}");
        listingSnapshotService.recordFetch(source, "2", "{}");
        listingSnapshotService.recordFetch("mark-removed-other-source", "1", "{}");
        Instant firstRemoval = Instant.now();

        assertThat(listingSnapshotService.markRemoved(source, List.of("1", "unknown"), firstRemoval)).isEqualTo(1);
        assertThat(listingSnapshotService.markRemoved(source, List.of("1"), firstRemoval.plusSeconds(60))).isEqualTo(0);
        assertThat(removedAt(source, "1")).isNotNull();
        assertThat(removedAt(source, "2")).isNull();
        assertThat(removedAt("mark-removed-other-source", "1")).isNull();
    }

    @Test
    void recordFetchReportsWhetherTheListingWasCreatedChangedOrUnchanged() {
        String source = "recorded-fetch-test";

        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"price\":100}")).isEqualTo(RecordedFetch.CREATED);
        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"price\":100}")).isEqualTo(RecordedFetch.UNCHANGED);
        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"price\":200}")).isEqualTo(RecordedFetch.CHANGED);
    }

    @Test
    void theSameJsonWithOtherWhitespaceOrKeyOrderIsUnchanged() {
        String source = "hash-normalised-test";
        listingSnapshotService.recordFetch(source, "1", "{\"a\": 1, \"b\": {\"c\": [1, 2]}}");

        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"b\":{\"c\":[1,2]},\"a\":1}"))
                .isEqualTo(RecordedFetch.UNCHANGED);
        assertThat(listingSnapshotService.recordFetch(source, "1", "{\"b\":{\"c\":[2,1]},\"a\":1}"))
                .isEqualTo(RecordedFetch.CHANGED);
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

    @Test
    void findExternalIdsReturnsEveryListingOfTheSourceIncludingRemovedOnes() {
        String source = "find-ids-test";
        listingSnapshotService.recordFetch(source, "1", "{}");
        listingSnapshotService.recordFetch(source, "2", "{}");
        listingSnapshotService.recordFetch("find-ids-other-source", "3", "{}");
        listingSnapshotService.markRemoved(source, List.of("2"), Instant.now());

        assertThat(removedAt(source, "2")).isNotNull();
        assertThat(listingSnapshotService.findExternalIds(source)).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void announcesOnlyFetchesThatWroteASnapshot() {
        String source = "event-test";
        listingSnapshotService.recordFetch(source, "1", "{\"price\":100}");
        listingSnapshotService.recordFetch(source, "1", "{\"price\":100}");
        listingSnapshotService.recordFetch(source, "1", "{\"price\":200}");

        assertThat(events.stream(ListingSnapshotRecorded.class))
                .containsExactly(new ListingSnapshotRecorded(source, "1"), new ListingSnapshotRecorded(source, "1"));
    }

    @Test
    void findSnapshotsReturnsEverySnapshotOldestFirst() {
        String source = "find-snapshots";
        listingSnapshotService.recordFetch(source, "1", "{\"price\": 100}");
        listingSnapshotService.recordFetch(source, "1", "{\"price\": 200}");

        assertThat(listingSnapshotService.findSnapshots(source, "1"))
                .extracting(ListingSnapshotView::payload)
                .containsExactly("{\"price\": 100}", "{\"price\": 200}");
        assertThat(listingSnapshotService.findSnapshots(source, "unknown")).isEmpty();
    }

    @Test
    void findListingReturnsPresenceTimestampsOrEmptyIfUnknown() {
        String source = "find-listing";
        listingSnapshotService.recordFetch(source, "1", "{\"price\": 100}");

        ListingView listing = listingSnapshotService.findListing(source, "1").orElseThrow();
        assertThat(listing.externalId()).isEqualTo("1");
        assertThat(listing.firstSeenAt()).isNotNull().isEqualTo(listing.lastSeenAt());
        assertThat(listing.removedAt()).isNull();
        assertThat(listingSnapshotService.findListing(source, "unknown")).isEmpty();
    }

    private Instant removedAt(String source, String externalId) {
        return listingRepository.findBySourceAndExternalId(source, externalId).orElseThrow().getRemovedAt();
    }
}
