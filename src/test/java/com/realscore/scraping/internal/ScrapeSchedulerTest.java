package com.realscore.scraping.internal;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realscore.scraping.ScrapeRunService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Time is a {@link FakeTime} that only moves when advanced; {@code run()} of the mocked job records a run started
 * "now", like the real job would.
 */
class ScrapeSchedulerTest {

    /** 13:00 and 21:00 Prague, as in docker-compose.yml. */
    private static final String CRON = "0 0 13,21 * * *";

    private final FakeTime time = new FakeTime();
    private final SrealityScrapeJob job = mock(SrealityScrapeJob.class);
    private final ScrapeRunService scrapeRunService = mock(ScrapeRunService.class);
    private final AtomicReference<Instant> latestStart = new AtomicReference<>();

    @BeforeEach
    void recordRunsStartedByTheJob() {
        when(scrapeRunService.findLatestStartNotAborted()).thenAnswer(_ -> Optional.ofNullable(latestStart.get()));
        doAnswer(_ -> {
            latestStart.set(time.instant());
            return null;
        }).when(job).run();
    }

    private ScrapeScheduler scheduler(String cron, Instant appStartedAt) {
        return new ScrapeScheduler(new ScrapeSchedule(cron), job, scrapeRunService, time, appStartedAt);
    }

    /** FakeTime starts at 2026-10-31T12:00:00Z, which is 13:00 in Prague (CET). */
    @Test
    void slotThatPassedWhileTheHostSleptIsRunAtTheFirstCheckAfterIt() {
        latestStart.set(Instant.parse("2026-10-30T20:00:00Z")); // 21:00 Prague the day before
        ScrapeScheduler scheduler = scheduler(CRON, time.instant());

        time.advance(Duration.ofMinutes(-1)); // 12:59 Prague: the 13:00 slot is not due yet
        scheduler.runIfDue();
        verify(job, never()).run();

        time.advance(Duration.ofHours(5)); // woke up at 17:59 Prague, hours after the 13:00 slot
        scheduler.runIfDue();
        verify(job, times(1)).run();
    }

    @Test
    void severalSlotsMissedDuringOneOutageStartOnlyOneRun() {
        latestStart.set(Instant.parse("2026-10-28T12:00:00Z"));
        ScrapeScheduler scheduler = scheduler(CRON, time.instant());

        scheduler.runIfDue();
        time.advance(Duration.ofMinutes(1));
        scheduler.runIfDue();
        time.advance(Duration.ofHours(2));
        scheduler.runIfDue();

        verify(job, times(1)).run();
    }

    @Test
    void nextSlotAfterACaughtUpRunStartsTheNextRun() {
        latestStart.set(Instant.parse("2026-10-28T12:00:00Z"));
        ScrapeScheduler scheduler = scheduler(CRON, time.instant());
        scheduler.runIfDue();

        time.advance(Duration.ofHours(8)); // 21:00 Prague
        scheduler.runIfDue();

        verify(job, times(2)).run();
    }

    @Test
    void runIsNotDueBeforeTheNextSlot() {
        latestStart.set(time.instant()); // the 13:00 run just started
        ScrapeScheduler scheduler = scheduler(CRON, time.instant());

        time.advance(Duration.ofHours(7).plusMinutes(59)); // 20:59 Prague
        scheduler.runIfDue();

        verify(job, never()).run();
    }

    @Test
    void runInProgressMeansNoSlotHasPassedSinceItsStart() {
        latestStart.set(time.instant().minus(Duration.ofMinutes(30))); // RUNNING since 12:30 Prague, not aborted
        ScrapeScheduler scheduler = scheduler("0 0 21 * * *", time.instant());

        scheduler.runIfDue();

        verify(job, never()).run();
    }

    @Test
    void withoutAnyRunSlotsCountFromTheAppStart() {
        ScrapeScheduler scheduler = scheduler(CRON, time.instant());

        scheduler.runIfDue();
        verify(job, never()).run();

        time.advance(Duration.ofHours(8));
        scheduler.runIfDue();
        verify(job, times(1)).run();
    }

    @Test
    void disabledCronNeverStartsARun() {
        ScrapeScheduler scheduler = scheduler(ScrapeSchedule.DISABLED, Instant.parse("2020-01-01T00:00:00Z"));

        scheduler.runIfDue();

        verify(job, never()).run();
        verify(scrapeRunService, never()).findLatestStartNotAborted();
    }
}
