package com.realscore.scraping.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.scraping.ScrapeRunService;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SrealityScrapeJobOverlapTest {

    @Test
    void callWhileARunIsInProgressStartsNoSecondRun() {
        SrealityBulkCollector bulkCollector = mock(SrealityBulkCollector.class);
        ScrapeRunService scrapeRunService = mock(ScrapeRunService.class);
        when(scrapeRunService.openRun()).thenReturn(1L);
        SrealityScrapeJob job = new SrealityScrapeJob(bulkCollector, scrapeRunService,
                new ScrapeJobProperties("-", Duration.ofMinutes(1), 5_000_000, null, Duration.ofDays(7),
                        Duration.ofDays(7)),
                mock(ListingSnapshotService.class), mock(SrealityRemovalCheck.class));
        AtomicInteger collections = new AtomicInteger();
        when(bulkCollector.collect(anyLong(), any(), any())).thenAnswer(_ -> {
            if (collections.incrementAndGet() == 1) {
                job.run(); // another start while the first run is still collecting
            }
            throw new IllegalStateException("stop here");
        });

        job.run();

        verify(scrapeRunService, times(1)).openRun();
        assertThat(collections).hasValue(1);

        job.run(); // after the first run finished, a new one starts again
        verify(scrapeRunService, times(2)).openRun();
    }
}
