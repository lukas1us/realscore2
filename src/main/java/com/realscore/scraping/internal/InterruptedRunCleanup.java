package com.realscore.scraping.internal;

import com.realscore.scraping.ScrapeRunService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Once the app is ready, closes as ABORTED every run left RUNNING by an earlier start of the app. Only runs that
 * started before this bean was created count, i.e. before the scheduler of this start could open one, so a run in
 * progress in this app is never aborted. Assumes one app instance per database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class InterruptedRunCleanup {

    private final Instant appStartedAt = Instant.now();
    private final ScrapeRunService scrapeRunService;

    @EventListener(ApplicationReadyEvent.class)
    void abortInterruptedRuns() {
        int aborted = scrapeRunService.abortInterruptedRuns(appStartedAt);
        if (aborted > 0) {
            log.warn("Closed {} scrape runs left RUNNING by an earlier start of the app as ABORTED", aborted);
        }
    }
}
