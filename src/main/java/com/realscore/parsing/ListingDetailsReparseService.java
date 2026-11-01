package com.realscore.parsing;

import com.realscore.listing.ListingSnapshotService;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Re-derives listing_detail rows from already-stored snapshots, e.g. after the parser changed. No scraping happens:
 * each listing's latest snapshot is parsed again. Each listing is parsed in its own transaction, so one snapshot the
 * parser can't handle is counted as failed without stopping the rest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingDetailsReparseService {

    private final ListingSnapshotService listingSnapshotService;
    private final ListingDetailsParsingService listingDetailsParsingService;

    /** Re-parses every stored listing from {@code source}, removed ones included. */
    public ReparseResult reparseAll(String source) {
        return reparse(source, listingSnapshotService.findExternalIds(source));
    }

    /** Re-parses the given listings from {@code source}; an ID without a stored snapshot counts as failed. */
    public ReparseResult reparse(String source, Collection<String> externalIds) {
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        for (String externalId : externalIds) {
            try {
                switch (listingDetailsParsingService.parse(source, externalId)) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case UNCHANGED -> unchanged++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("Re-parsing {}/{} failed", source, externalId, e);
            }
        }
        ReparseResult result = new ReparseResult(created, updated, unchanged, failed);
        log.info("Re-parsed {} listings from {}: {}", externalIds.size(), source, result);
        return result;
    }
}
