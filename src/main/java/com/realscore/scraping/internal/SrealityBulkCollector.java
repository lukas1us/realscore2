package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.RecordedFetch;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * The end-to-end bulk path: collects every sale-apartment ID under a price cap, then fetches and records the detail
 * of each one that has no listing yet and of each known one last fetched more than {@code refetchAfter} ago.
 * Requests go through {@link SrealityRequestPolicy}, so retries and pacing apply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrealityBulkCollector {

    private final SrealityIdCollector idCollector;
    private final SrealityEstateFetcher fetcher;
    private final ListingSnapshotService listingSnapshotService;

    /**
     * A failure for one estate is counted and logged, and the run goes on with the next one.
     *
     * @param priceCap maximum price in CZK
     * @param refetchAfter a known listing is fetched again once its last fetch is older than this; zero means always
     * @throws SrealityApiException if collecting the IDs fails; nothing is fetched then
     */
    public BulkCollectionResult collect(long priceCap, Duration refetchAfter) {
        Set<Long> found = idCollector.collectSaleApartmentIds(priceCap);
        List<String> foundExternalIds = found.stream().map(String::valueOf).toList();
        Set<String> known = listingSnapshotService.findKnownExternalIds(SrealityEstateFetcher.SOURCE, foundExternalIds);
        Set<String> due = listingSnapshotService.findExternalIdsFetchedBefore(
                SrealityEstateFetcher.SOURCE, foundExternalIds, Instant.now().minus(refetchAfter));
        List<Long> newIds = found.stream().filter(id -> !known.contains(String.valueOf(id))).toList();
        List<Long> dueIds = found.stream().filter(id -> due.contains(String.valueOf(id))).toList();
        log.info("Sreality bulk collection with price cap {}: {} found, {} already known, {} new and {} due to refetch",
                priceCap, found.size(), known.size(), newIds.size(), dueIds.size());

        int fetched = 0;
        int refetched = 0;
        int changed = 0;
        int skipped = 0;
        int failed = 0;
        for (long estateId : Stream.concat(newIds.stream(), dueIds.stream()).toList()) {
            try {
                Optional<RecordedFetch> recorded = fetcher.fetch(estateId);
                if (recorded.isEmpty()) {
                    skipped++;
                    continue;
                }
                switch (recorded.get()) {
                    case CREATED -> fetched++;
                    case CHANGED -> {
                        refetched++;
                        changed++;
                    }
                    case UNCHANGED -> refetched++;
                }
            } catch (SrealityApiException | RestClientException | IllegalStateException e) {
                failed++;
                log.warn("Fetching Sreality estate {} failed; continuing with the next one", estateId, e);
            }
        }
        BulkCollectionResult result =
                new BulkCollectionResult(found, known.size(), fetched, refetched, changed, skipped, failed);
        log.info("Sreality bulk collection finished: {}", result);
        return result;
    }
}
