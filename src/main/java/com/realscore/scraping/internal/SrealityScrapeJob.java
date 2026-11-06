package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.scraping.ScrapeRunCounts;
import com.realscore.scraping.ScrapeRunService;
import com.realscore.scraping.ScrapeRunStatus;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Runs the Sreality bulk collection and records every execution as a {@code scrape_run}. {@link ScrapeScheduler}
 * decides when.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrealityScrapeJob {

    private final SrealityBulkCollector bulkCollector;
    private final ScrapeRunService scrapeRunService;
    private final ScrapeJobProperties properties;
    private final ListingSnapshotService listingSnapshotService;
    private final SrealityRemovalCheck removalCheck;
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * Never throws: a failure closes the run as {@link ScrapeRunStatus#FAILED} with the cause as error details, and
     * marks nothing removed. After a successful run, every active listing it did not see is checked by
     * {@link SrealityRemovalCheck}, and only those whose detail is gone are marked removed. A successful run that found
     * no IDs at all checks nothing, since that more likely means Sreality misbehaved than that every listing is gone.
     * A call while a run is in progress returns at once and opens no run.
     */
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("A scrape run is already in progress; not starting another");
            return;
        }
        try {
            runOnce();
        } finally {
            running.set(false);
        }
    }

    private void runOnce() {
        Instant startedAt = Instant.now();
        Long runId = scrapeRunService.openRun();
        log.info("Scrape run {} started, sale price cap {}, rent price cap {}", runId, properties.priceCap(),
                properties.rentPriceCap());
        try {
            BulkCollectionResult result = bulkCollector.collect(properties.priceCap(), properties.rentPriceCap(),
                    properties.refetchAfter());
            RemovalCheckResult check = recordPresenceAndCheckUnseen(runId, result, startedAt);
            scrapeRunService.updateCounts(runId, new ScrapeRunCounts(result.found(), result.fetched(),
                    result.changed() + check.changed(), check.removed(), result.skipped(), result.alreadyKnown(),
                    result.failed() + check.failed()));
            scrapeRunService.closeRun(runId, ScrapeRunStatus.SUCCEEDED, null);
            log.info("Scrape run {} succeeded: {}, {}", runId, result, check);
        } catch (RuntimeException e) {
            log.error("Scrape run {} failed", runId, e);
            scrapeRunService.closeRun(runId, ScrapeRunStatus.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private RemovalCheckResult recordPresenceAndCheckUnseen(Long runId, BulkCollectionResult result, Instant startedAt) {
        if (result.foundIds().isEmpty()) {
            log.warn("Scrape run {} found no estates at all; not checking or marking any listing removed", runId);
            return new RemovalCheckResult(0, 0, 0, 0, 0);
        }
        int reappeared = listingSnapshotService.recordPresence(SrealityEstateFetcher.SOURCE,
                result.foundIds().stream().map(String::valueOf).toList());
        log.info("Scrape run {}: {} listings reappeared", runId, reappeared);
        return removalCheck.check(startedAt);
    }
}
