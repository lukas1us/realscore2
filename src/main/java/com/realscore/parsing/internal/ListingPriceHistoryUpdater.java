package com.realscore.parsing.internal;

import com.realscore.listing.ListingSnapshotView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Derives a listing's price history from all its snapshots and stores it in listing_price_point. The history is
 * rebuilt from scratch each time, so it is idempotent and picks up parser changes on a re-parse.
 */
@Component
@RequiredArgsConstructor
public class ListingPriceHistoryUpdater {

    private final SrealityListingDetailsExtractor extractor;
    private final ListingPricePointRepository repository;

    /**
     * Must run inside a transaction.
     *
     * @param snapshots every snapshot of one listing, oldest first
     * @return whether the stored history changed
     */
    public boolean rebuild(Long listingId, List<ListingSnapshotView> snapshots) {
        List<PricePoint> derived = derive(snapshots);
        List<PricePoint> stored = repository.findByListingIdOrderByObservedAtAsc(listingId).stream()
                .map(point -> new PricePoint(point.getObservedAt(), point.getPriceCzk()))
                .toList();
        if (same(stored, derived)) {
            return false;
        }
        repository.deleteAllByListingId(listingId);
        repository.saveAll(derived.stream()
                .map(point -> new ListingPricePoint(listingId, point.observedAt(), point.priceCzk()))
                .toList());
        return true;
    }

    /** One point per price change; snapshots without a price ("price on request") are skipped. */
    private List<PricePoint> derive(List<ListingSnapshotView> snapshots) {
        List<PricePoint> points = new ArrayList<>();
        for (ListingSnapshotView snapshot : snapshots) {
            BigDecimal price = extractor.extract(snapshot.payload()).priceCzk();
            if (price == null) {
                continue;
            }
            if (points.isEmpty() || points.getLast().priceCzk().compareTo(price) != 0) {
                points.add(new PricePoint(snapshot.fetchedAt(), price));
            }
        }
        return points;
    }

    /** Compares decimals by value, so a NUMERIC scale change from the DB isn't a change. */
    private static boolean same(List<PricePoint> a, List<PricePoint> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).observedAt().equals(b.get(i).observedAt())
                    || a.get(i).priceCzk().compareTo(b.get(i).priceCzk()) != 0) {
                return false;
            }
        }
        return true;
    }

    private record PricePoint(Instant observedAt, BigDecimal priceCzk) {
    }
}
