package com.realscore.scraping.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.scraping.ScrapeRunService;
import com.realscore.scraping.ScrapeRunStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class InterruptedRunCleanupTest {

    @Autowired
    private InterruptedRunCleanup cleanup;

    @Autowired
    private ScrapeRunService scrapeRunService;

    @Autowired
    private ScrapeRunRepository scrapeRunRepository;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Test
    void runLeftRunningByAnEarlierStartIsAbortedWithAFinishedTimeAndReason() {
        Long interrupted = scrapeRunRepository.save(new ScrapeRun(Instant.now().minus(Duration.ofHours(3)))).getId();

        Instant before = Instant.now();
        cleanup.abortInterruptedRuns();

        ScrapeRun run = scrapeRunRepository.findById(interrupted).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(ScrapeRunStatus.ABORTED);
        assertThat(run.getFinishedAt()).isAfterOrEqualTo(before);
        assertThat(run.getErrorDetails()).isEqualTo(ScrapeRunService.ABORTED_REASON);
    }

    @Test
    void runInProgressInThisAppIsNotAborted() {
        Long inProgress = scrapeRunService.openRun();

        cleanup.abortInterruptedRuns();

        ScrapeRun run = scrapeRunRepository.findById(inProgress).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(ScrapeRunStatus.RUNNING);
        assertThat(run.getFinishedAt()).isNull();
        assertThat(run.getErrorDetails()).isNull();
        scrapeRunService.closeRun(inProgress, ScrapeRunStatus.SUCCEEDED, null);
    }

    @Test
    void finishedRunsAreLeftAsTheyAre() {
        ScrapeRun failed = new ScrapeRun(Instant.now().minus(Duration.ofDays(1)));
        failed.setStatus(ScrapeRunStatus.FAILED);
        failed.setErrorDetails("boom");
        Long failedId = scrapeRunRepository.save(failed).getId();

        cleanup.abortInterruptedRuns();

        ScrapeRun run = scrapeRunRepository.findById(failedId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(ScrapeRunStatus.FAILED);
        assertThat(run.getErrorDetails()).isEqualTo("boom");
    }

    @Test
    void abortingARunMarksNoListingRemoved() {
        String externalId = "48000001";
        listingSnapshotService.recordFetch(SrealityEstateFetcher.SOURCE, externalId, "{\"result\":{}}");
        scrapeRunRepository.save(new ScrapeRun(Instant.now().minus(Duration.ofHours(1))));

        cleanup.abortInterruptedRuns();

        assertThat(listingSnapshotService.findListing(SrealityEstateFetcher.SOURCE, externalId).orElseThrow()
                .removedAt()).isNull();
    }
}
