package com.realscore.scraping.internal;

import com.realscore.scraping.ScrapeRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scrape_run")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScrapeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScrapeRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "found_count", nullable = false)
    private int foundCount;

    @Column(name = "new_count", nullable = false)
    private int newCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "removed_count", nullable = false)
    private int removedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "already_known_count", nullable = false)
    private int alreadyKnownCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_details")
    private String errorDetails;

    public ScrapeRun(Instant startedAt) {
        this.status = ScrapeRunStatus.RUNNING;
        this.startedAt = startedAt;
    }
}
