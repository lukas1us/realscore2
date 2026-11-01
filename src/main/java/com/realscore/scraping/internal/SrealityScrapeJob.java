package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.PresenceResult;
import com.realscore.scraping.ScrapeRunCounts;
import com.realscore.scraping.ScrapeRunService;
import com.realscore.scraping.ScrapeRunStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the Sreality bulk collection on the configured schedule and records every execution as a {@code scrape_run}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrealityScrapeJob {

    static final String ZONE = "Europe/Prague";

    private final SrealityBulkCollector bulkCollector;
    private final ScrapeRunService scrapeRunService;
    private final ScrapeJobProperties properties;
    private final ListingSnapshotService listingSnapshotService;

    /**
     * Never throws: a failure closes the run as {@link ScrapeRunStatus#FAILED} with the cause as error details, and
     * marks nothing removed. A successful run that found no IDs at all marks nothing removed either, since that more
     * likely means Sreality misbehaved than that every listing is gone.
     */
    @Scheduled(cron = "${realscore.scrape.cron}", zone = ZONE)
    public void run() {
        Instant startedAt = Instant.now();
        Long runId = scrapeRunService.openRun();
        log.info("Scrape run {} started, sale price cap {}, rent price cap {}", runId, properties.priceCap(),
                properties.rentPriceCap());
        try {
            BulkCollectionResult result = bulkCollector.collect(properties.priceCap(), properties.rentPriceCap(),
                    properties.refetchAfter());
            int removed = recordPresence(runId, result, startedAt);
            scrapeRunService.updateCounts(runId, new ScrapeRunCounts(result.found(), result.fetched(),
                    result.changed(), removed, result.skipped(), result.alreadyKnown(), result.failed()));
            scrapeRunService.closeRun(runId, ScrapeRunStatus.SUCCEEDED, null);
            log.info("Scrape run {} succeeded: {}, {} removed", runId, result, removed);
        } catch (RuntimeException e) {
            log.error("Scrape run {} failed", runId, e);
            scrapeRunService.closeRun(runId, ScrapeRunStatus.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private int recordPresence(Long runId, BulkCollectionResult result, Instant startedAt) {
        if (result.foundIds().isEmpty()) {
            log.warn("Scrape run {} found no estates at all; not marking any listing removed", runId);
            return 0;
        }
        PresenceResult presence = listingSnapshotService.recordPresence(SrealityEstateFetcher.SOURCE,
                result.foundIds().stream().map(String::valueOf).toList(), startedAt);
        log.info("Scrape run {}: {} listings reappeared, {} marked removed", runId, presence.reappeared(),
                presence.removed());
        return presence.removed();
    }
}
