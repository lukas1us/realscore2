package com.realscore.scraping;

import com.realscore.scraping.internal.ScrapeRun;
import com.realscore.scraping.internal.ScrapeRunRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScrapeRunService {

    private final ScrapeRunRepository scrapeRunRepository;

    ScrapeRunService(ScrapeRunRepository scrapeRunRepository) {
        this.scrapeRunRepository = scrapeRunRepository;
    }

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

    private ScrapeRun getRun(Long runId) {
        return scrapeRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("No scrape_run with id " + runId));
    }
}
