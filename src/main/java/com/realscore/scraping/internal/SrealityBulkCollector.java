package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.RecordedFetch;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * The end-to-end bulk path: collects the ID and search price of every apartment for sale and for rent (each
 * optionally under its own price cap), then fetches and records the detail of each one that has no listing yet, of
 * each known one last fetched more than {@code refetchAfter} ago, and of each known one whose search price differs
 * from the one stored by an earlier run. Requests go through {@link SrealityRequestPolicy}, so retries and pacing
 * apply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrealityBulkCollector {

    private final SrealityIdCollector idCollector;
    private final SrealityEstateFetcher fetcher;
    private final ListingSnapshotService listingSnapshotService;

    /**
     * A failure for one estate is counted and logged, and the run goes on with the next one. Afterwards the search
     * price of every found listing is stored for the next run to compare with, except for listings whose fetch was
     * skipped or failed: they keep their old search price, so a changed price is retried on the next run.
     *
     * @param salePriceCap maximum price in CZK of apartments for sale
     * @param rentPriceCap maximum monthly rent in CZK of apartments for rent, or {@code null} for no cap
     * @param refetchAfter a known listing is fetched again once its last fetch is older than this; zero means always
     * @throws SrealityApiException if collecting the IDs fails; nothing is fetched then
     */
    public BulkCollectionResult collect(long salePriceCap, Long rentPriceCap, Duration refetchAfter) {
        Map<Long, BigDecimal> sales = idCollector.collectSaleApartments(salePriceCap);
        Map<Long, BigDecimal> rentals = idCollector.collectRentalApartments(rentPriceCap);
        Map<Long, BigDecimal> found = new LinkedHashMap<>(sales);
        rentals.forEach(found::putIfAbsent);
        List<String> foundExternalIds = found.keySet().stream().map(String::valueOf).toList();
        Set<String> known = listingSnapshotService.findKnownExternalIds(SrealityEstateFetcher.SOURCE, foundExternalIds);
        Set<String> due = listingSnapshotService.findExternalIdsFetchedBefore(
                SrealityEstateFetcher.SOURCE, foundExternalIds, Instant.now().minus(refetchAfter));
        Map<String, BigDecimal> storedPrices =
                listingSnapshotService.findSearchPrices(SrealityEstateFetcher.SOURCE, foundExternalIds);
        List<Long> newIds = found.keySet().stream().filter(id -> !known.contains(String.valueOf(id))).toList();
        List<Long> dueIds = found.keySet().stream().filter(id -> due.contains(String.valueOf(id))).toList();
        List<Long> priceChangedIds = found.keySet().stream()
                .filter(id -> known.contains(String.valueOf(id)) && !due.contains(String.valueOf(id)))
                .filter(id -> priceChanged(storedPrices.get(String.valueOf(id)), found.get(id)))
                .toList();
        log.info("Sreality bulk collection with sale price cap {} and rent price cap {}: {} found ({} for sale, {} for"
                        + " rent), {} already known, {} new, {} due to refetch and {} with a changed search price",
                salePriceCap, rentPriceCap, found.size(), sales.size(), rentals.size(), known.size(),
                newIds.size(), dueIds.size(), priceChangedIds.size());

        Set<Long> toFetch = new LinkedHashSet<>(newIds);
        toFetch.addAll(dueIds);
        toFetch.addAll(priceChangedIds);
        Set<Long> notRecorded = new HashSet<>();
        int fetched = 0;
        int refetched = 0;
        int changed = 0;
        int skipped = 0;
        int failed = 0;
        for (long estateId : toFetch) {
            try {
                Optional<RecordedFetch> recorded = fetcher.fetch(estateId);
                if (recorded.isEmpty()) {
                    skipped++;
                    notRecorded.add(estateId);
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
                notRecorded.add(estateId);
                log.warn("Fetching Sreality estate {} failed; continuing with the next one", estateId, e);
            }
        }
        recordSearchPrices(found, storedPrices, notRecorded);

        BulkCollectionResult result = new BulkCollectionResult(found.keySet(), known.size(), fetched, refetched,
                changed, priceChangedIds.size(), skipped, failed);
        log.info("Sreality bulk collection finished: {}", result);
        return result;
    }

    /** No stored price (none recorded yet) or no price in the search result is no evidence of a change. */
    private static boolean priceChanged(BigDecimal stored, BigDecimal searched) {
        return stored != null && searched != null && stored.compareTo(searched) != 0;
    }

    /** Stores only prices that differ from the stored ones, so a run without price changes writes nothing. */
    private void recordSearchPrices(Map<Long, BigDecimal> found, Map<String, BigDecimal> storedPrices,
            Set<Long> notRecorded) {
        Map<String, BigDecimal> updates = new HashMap<>();
        found.forEach((id, price) -> {
            if (price == null || notRecorded.contains(id)) {
                return;
            }
            BigDecimal stored = storedPrices.get(String.valueOf(id));
            if (stored == null || stored.compareTo(price) != 0) {
                updates.put(String.valueOf(id), price);
            }
        });
        listingSnapshotService.recordSearchPrices(SrealityEstateFetcher.SOURCE, updates);
    }
}
