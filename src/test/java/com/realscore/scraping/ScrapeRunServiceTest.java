package com.realscore.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.scraping.internal.ScrapeRun;
import com.realscore.scraping.internal.ScrapeRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class ScrapeRunServiceTest {

    @Autowired
    private ScrapeRunService scrapeRunService;

    @Autowired
    private ScrapeRunRepository scrapeRunRepository;

    @Test
    void opensUpdatesAndClosesARun() {
        Long runId = scrapeRunService.openRun();

        ScrapeRun opened = scrapeRunRepository.findById(runId).orElseThrow();
        assertThat(opened.getStatus()).isEqualTo(ScrapeRunStatus.RUNNING);
        assertThat(opened.getStartedAt()).isNotNull();
        assertThat(opened.getFinishedAt()).isNull();

        scrapeRunService.updateCounts(runId, 10, 3, 2, 1, 0);

        ScrapeRun updated = scrapeRunRepository.findById(runId).orElseThrow();
        assertThat(updated.getFoundCount()).isEqualTo(10);
        assertThat(updated.getNewCount()).isEqualTo(3);
        assertThat(updated.getUpdatedCount()).isEqualTo(2);
        assertThat(updated.getRemovedCount()).isEqualTo(1);
        assertThat(updated.getErrorCount()).isEqualTo(0);

        scrapeRunService.closeRun(runId, ScrapeRunStatus.SUCCEEDED, null);

        ScrapeRun closed = scrapeRunRepository.findById(runId).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(ScrapeRunStatus.SUCCEEDED);
        assertThat(closed.getFinishedAt()).isNotNull();
        assertThat(closed.getErrorDetails()).isNull();
    }

    @Test
    void closingWithFailedStatusRecordsErrorDetails() {
        Long runId = scrapeRunService.openRun();

        scrapeRunService.closeRun(runId, ScrapeRunStatus.FAILED, "connection timed out");

        ScrapeRun closed = scrapeRunRepository.findById(runId).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(ScrapeRunStatus.FAILED);
        assertThat(closed.getErrorDetails()).isEqualTo("connection timed out");
    }
}
