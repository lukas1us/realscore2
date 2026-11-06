package com.realscore.scraping.internal;

import com.realscore.scraping.ScrapeRunService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Starts {@link SrealityScrapeJob} when a slot of the configured cron has passed without a run. Spring's own cron
 * scheduling waits out its delays on the monotonic clock, which stops while the host sleeps, so runs fired hours late
 * or not at all. Instead this checks every {@code realscore.scrape.check-interval} against the wall clock: a run is
 * due once the first slot after the latest run's start has passed. However many slots passed during an outage, that
 * starts one run, after which the next slot is in the future again. Runs closed as ABORTED do not count, so a run
 * interrupted by a restart is caught up. Before the first run ever, slots count from the app's start.
 *
 * <p>Checks run one after another on the scheduler thread and a check waits for the run it started, so a check never
 * starts a run while another is in progress; {@link SrealityScrapeJob#run()} guards against overlap as well.
 */
@Slf4j
@RequiredArgsConstructor
class ScrapeScheduler {

    private final ScrapeSchedule schedule;
    private final SrealityScrapeJob job;
    private final ScrapeRunService scrapeRunService;
    private final Clock clock;
    private final Instant appStartedAt;

    @Scheduled(fixedDelayString = "${realscore.scrape.check-interval}")
    void runIfDue() {
        if (!schedule.isEnabled()) {
            return;
        }
        Instant now = clock.instant();
        Instant lastStart = scrapeRunService.findLatestStartNotAborted().orElse(appStartedAt);
        Optional<Instant> slot = schedule.nextSlotAfter(lastStart).filter(due -> !due.isAfter(now));
        if (slot.isPresent()) {
            log.info("Scrape run due since {} (last run started {}); starting it",
                    slot.get().atZone(ScrapeSchedule.ZONE), lastStart.atZone(ScrapeSchedule.ZONE));
            job.run();
        }
    }
}
