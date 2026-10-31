package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * The end-to-end bulk path: collects every sale-apartment ID under a price cap and fetches and records the detail of
 * each one that has no listing yet. Requests go through {@link SrealityRequestPolicy}, so retries and pacing apply.
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
     * @throws SrealityApiException if collecting the IDs fails; nothing is fetched then
     */
    public BulkCollectionResult collect(long priceCap) {
        Set<Long> found = idCollector.collectSaleApartmentIds(priceCap);
        Set<String> known = listingSnapshotService.findKnownExternalIds(SrealityEstateFetcher.SOURCE,
                found.stream().map(String::valueOf).toList());
        List<Long> newIds = found.stream().filter(id -> !known.contains(String.valueOf(id))).toList();
        log.info("Sreality bulk collection with price cap {}: {} found, {} already known, {} to fetch",
                priceCap, found.size(), known.size(), newIds.size());

        int fetched = 0;
        int skipped = 0;
        int failed = 0;
        for (long estateId : newIds) {
            try {
                if (fetcher.fetch(estateId)) {
                    fetched++;
                } else {
                    skipped++;
                }
            } catch (SrealityApiException | RestClientException | IllegalStateException e) {
                failed++;
                log.warn("Fetching Sreality estate {} failed; continuing with the next one", estateId, e);
            }
        }
        BulkCollectionResult result = new BulkCollectionResult(found.size(), known.size(), fetched, skipped, failed);
        log.info("Sreality bulk collection finished: {}", result);
        return result;
    }
}
