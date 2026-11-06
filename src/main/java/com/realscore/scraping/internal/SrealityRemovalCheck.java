package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.RecordedFetch;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Decides whether listings a run did not see are really gone. Dropping out of the search results is no proof: a
 * listing whose price rose above the price cap, or whose category changed, is still online. So the detail of each
 * one is fetched, and only a 404 or 410 marks the listing removed. One still online is marked out of scope, which
 * keeps it out of the analysis, and is checked again only every {@code outOfScopeRecheckAfter} instead of every run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrealityRemovalCheck {

    private final SrealityClient client;
    private final ListingSnapshotService listingSnapshotService;
    private final ScrapeJobProperties properties;

    /**
     * Fetches the detail of every active Sreality listing not seen since {@code notSeenSince}, except listings
     * already marked out of scope whose detail was fetched within {@code outOfScopeRecheckAfter}. HTTP 200 records
     * the snapshot, which moves {@code last_seen_at} and keeps the listing active, and marks it out of scope if it is
     * not yet; 404 and 410 mark it removed; 403 and any other failure leave it as it is, so the next run checks it
     * again. A failure for one listing is logged and the check goes on with the next one.
     */
    public RemovalCheckResult check(Instant notSeenSince) {
        List<String> unseen = listingSnapshotService.findActiveExternalIdsNotSeenSince(SrealityEstateFetcher.SOURCE,
                notSeenSince, Instant.now().minus(properties.outOfScopeRecheckAfter()));
        log.info("Checking the detail of {} active Sreality listings the run did not see", unseen.size());

        int kept = 0;
        int changed = 0;
        int failed = 0;
        List<String> gone = new ArrayList<>();
        List<String> online = new ArrayList<>();
        for (String externalId : unseen) {
            try {
                switch (client.fetchDetail(Long.parseLong(externalId))) {
                    case EstateDetail.Found found -> {
                        kept++;
                        online.add(externalId);
                        RecordedFetch recorded = listingSnapshotService.recordFetch(
                                SrealityEstateFetcher.SOURCE, externalId, found.body());
                        if (recorded == RecordedFetch.CHANGED) {
                            changed++;
                        }
                    }
                    case EstateDetail.Gone _ -> gone.add(externalId);
                    case EstateDetail.Forbidden _ -> {
                        failed++;
                        log.warn("Sreality answered 403 for unseen listing {}; leaving it active", externalId);
                    }
                }
            } catch (SrealityApiException | RestClientException | IllegalStateException e) {
                failed++;
                log.warn("Checking unseen Sreality listing {} failed; leaving it active", externalId, e);
            }
        }
        int removed = listingSnapshotService.markRemoved(SrealityEstateFetcher.SOURCE, gone, Instant.now());
        int outOfScope = listingSnapshotService.markOutOfScope(SrealityEstateFetcher.SOURCE, online, Instant.now());
        log.info("{} of the {} unseen listings still online are newly marked out of scope", outOfScope, kept);
        RemovalCheckResult result = new RemovalCheckResult(unseen.size(), kept, changed, removed, failed);
        log.info("Removal check finished: {}", result);
        return result;
    }
}
