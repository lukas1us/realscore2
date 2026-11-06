package com.realscore.scraping;

import com.realscore.scraping.internal.ScrapeRun;
import com.realscore.scraping.internal.ScrapeRunRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ScrapeRunService {

    public static final String ABORTED_REASON = "Interrupted: the app stopped while the run was in progress";

    private final ScrapeRunRepository scrapeRunRepository;

    @Transactional
    public Long openRun() {
        ScrapeRun run = scrapeRunRepository.save(new ScrapeRun(Instant.now()));
        return run.getId();
    }

    @Transactional
    public void updateCounts(Long runId, ScrapeRunCounts counts) {
        ScrapeRun run = getRun(runId);
        run.setFoundCount(counts.found());
        run.setNewCount(counts.newCount());
        run.setUpdatedCount(counts.updated());
        run.setRemovedCount(counts.removed());
        run.setSkippedCount(counts.skipped());
        run.setAlreadyKnownCount(counts.alreadyKnown());
        run.setErrorCount(counts.errors());
    }

    @Transactional
    public void closeRun(Long runId, ScrapeRunStatus status, String errorDetails) {
        ScrapeRun run = getRun(runId);
        run.setStatus(status);
        run.setFinishedAt(Instant.now());
        run.setErrorDetails(errorDetails);
    }

    /**
     * Closes every run still {@link ScrapeRunStatus#RUNNING} that started before {@code startedBefore} as
     * {@link ScrapeRunStatus#ABORTED}, with a finished time of now and {@link #ABORTED_REASON} as error details. Its
     * counts stay as they were, and since the removal check comes last in a run, nothing was marked removed for it.
     *
     * @return how many runs were aborted
     */
    @Transactional
    public int abortInterruptedRuns(Instant startedBefore) {
        return scrapeRunRepository.closeRunsStartedBefore(ScrapeRunStatus.RUNNING, startedBefore,
                ScrapeRunStatus.ABORTED, Instant.now(), ABORTED_REASON);
    }

    /** @return when the latest run that was not {@link ScrapeRunStatus#ABORTED} started, or empty if none */
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestStartNotAborted() {
        return scrapeRunRepository.findLatestStartedAtExcluding(ScrapeRunStatus.ABORTED);
    }

    private ScrapeRun getRun(Long runId) {
        return scrapeRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("No scrape_run with id " + runId));
    }
}
