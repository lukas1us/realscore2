package com.realscore.scraping.internal;

import com.realscore.scraping.ScrapeRunService;
import com.realscore.scraping.ScrapeRunStatus;
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

    /**
     * Never throws: a failure closes the run as {@link ScrapeRunStatus#FAILED} with the cause as error details.
     */
    @Scheduled(cron = "${realscore.scrape.cron}", zone = ZONE)
    public void run() {
        Long runId = scrapeRunService.openRun();
        log.info("Scrape run {} started, price cap {}", runId, properties.priceCap());
        try {
            BulkCollectionResult result = bulkCollector.collect(properties.priceCap());
            scrapeRunService.updateCounts(runId, result.found(), result.fetched(), 0, 0, result.failed());
            scrapeRunService.closeRun(runId, ScrapeRunStatus.SUCCEEDED, null);
            log.info("Scrape run {} succeeded: {}", runId, result);
        } catch (RuntimeException e) {
            log.error("Scrape run {} failed", runId, e);
            scrapeRunService.closeRun(runId, ScrapeRunStatus.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
