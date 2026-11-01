package com.realscore.parsing.internal;

import com.realscore.listing.ListingSnapshotRecorded;
import com.realscore.parsing.ListingDetailsParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Parses every newly written snapshot into its listing_detail row, so scraped listings need no manual re-parse.
 * Runs after the snapshot's transaction commits; {@code NOT_SUPPORTED} suspends that finished transaction so
 * {@link ListingDetailsParsingService#parse} gets a fresh one of its own. A failure is logged and swallowed: the
 * snapshot is already stored, and the listing can be fixed later with a re-parse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SnapshotRecordedListener {

    private final ListingDetailsParsingService listingDetailsParsingService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void onSnapshotRecorded(ListingSnapshotRecorded event) {
        try {
            listingDetailsParsingService.parse(event.source(), event.externalId());
        } catch (RuntimeException e) {
            log.warn("Parsing new snapshot of {}/{} failed", event.source(), event.externalId(), e);
        }
    }
}
