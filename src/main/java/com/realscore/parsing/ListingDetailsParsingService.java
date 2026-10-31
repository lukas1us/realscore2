package com.realscore.parsing;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.ListingSnapshotView;
import com.realscore.parsing.internal.ListingDetail;
import com.realscore.parsing.internal.ListingDetailRepository;
import com.realscore.parsing.internal.ListingDetailsFields;
import com.realscore.parsing.internal.SrealityListingDetailsExtractor;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Normalises a listing's latest stored snapshot into structured listing_detail fields
 * (the "store raw first, parse separately and idempotently" decision). Re-running this
 * for the same listing overwrites its listing_detail row rather than duplicating it.
 */
@Service
@RequiredArgsConstructor
public class ListingDetailsParsingService {

    private final ListingSnapshotService listingSnapshotService;
    private final SrealityListingDetailsExtractor extractor;
    private final ListingDetailRepository listingDetailRepository;

    @Transactional
    public void parse(String source, String externalId) {
        ListingSnapshotView snapshot = listingSnapshotService.findLatestSnapshot(source, externalId)
                .orElseThrow(() -> new IllegalStateException(
                        "No snapshot found for " + source + "/" + externalId));

        ListingDetailsFields fields = extractor.extract(snapshot.payload());

        ListingDetail detail = listingDetailRepository.findByListingId(snapshot.listingId())
                .orElseGet(() -> new ListingDetail(snapshot.listingId()));
        detail.applyFields(fields, Instant.now());
        listingDetailRepository.save(detail);
    }
}
