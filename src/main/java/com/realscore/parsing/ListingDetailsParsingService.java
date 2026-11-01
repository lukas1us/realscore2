package com.realscore.parsing;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.ListingSnapshotView;
import com.realscore.parsing.internal.ListingDetail;
import com.realscore.parsing.internal.ListingDetailRepository;
import com.realscore.parsing.internal.ListingDetailsFields;
import com.realscore.parsing.internal.ListingPriceHistoryUpdater;
import com.realscore.parsing.internal.SrealityListingDetailsExtractor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Normalises a listing's latest stored snapshot into structured listing_detail fields
 * (the "store raw first, parse separately and idempotently" decision). Re-running this
 * for the same listing overwrites its listing_detail row rather than duplicating it, and leaves it untouched
 * (including parsed_at) when the parsed fields haven't changed. It also rebuilds the listing's price history
 * (listing_price_point) from all its snapshots.
 */
@Service
@RequiredArgsConstructor
public class ListingDetailsParsingService {

    private final ListingSnapshotService listingSnapshotService;
    private final SrealityListingDetailsExtractor extractor;
    private final ListingDetailRepository listingDetailRepository;
    private final ListingPriceHistoryUpdater priceHistoryUpdater;

    /** @return what happened to the listing_detail row; price history changes are not reflected */
    @Transactional
    public ParseOutcome parse(String source, String externalId) {
        List<ListingSnapshotView> snapshots = listingSnapshotService.findSnapshots(source, externalId);
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("No snapshot found for " + source + "/" + externalId);
        }
        ListingSnapshotView snapshot = snapshots.getLast();
        priceHistoryUpdater.rebuild(snapshot.listingId(), snapshots);

        ListingDetailsFields fields = extractor.extract(snapshot.payload());

        Optional<ListingDetail> existing = listingDetailRepository.findByListingId(snapshot.listingId());
        ListingDetail detail = existing.orElseGet(() -> new ListingDetail(snapshot.listingId()));
        if (!detail.applyFields(fields, Instant.now())) {
            return ParseOutcome.UNCHANGED;
        }
        listingDetailRepository.save(detail);
        return existing.isPresent() ? ParseOutcome.UPDATED : ParseOutcome.CREATED;
    }
}
