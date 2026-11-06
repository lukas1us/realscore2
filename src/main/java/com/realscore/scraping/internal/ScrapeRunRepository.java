package com.realscore.scraping.internal;

import com.realscore.scraping.ScrapeRunStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {

    /** Closes every run still in {@code from} that started before {@code startedBefore} with status {@code to}. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ScrapeRun r set r.status = :to, r.finishedAt = :finishedAt, r.errorDetails = :errorDetails
            where r.status = :from and r.startedAt < :startedBefore""")
    int closeRunsStartedBefore(ScrapeRunStatus from, Instant startedBefore, ScrapeRunStatus to, Instant finishedAt,
            String errorDetails);

    @Query("select max(r.startedAt) from ScrapeRun r where r.status <> :excluded")
    Optional<Instant> findLatestStartedAtExcluding(ScrapeRunStatus excluded);
}
