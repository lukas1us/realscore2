package com.realscore.service;

import com.realscore.model.Property;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyScoringServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final PropertyScoringService scoringService = new PropertyScoringService(fixedClock);

    @Test
    void scoresNewAffordableWellSizedPropertyHighly() {
        Property property = new Property("1 Main St", 300_000, 3, 2, 2000, 2025);

        double score = scoringService.score(property);

        assertThat(score).isGreaterThan(80);
    }

    @Test
    void scoresExpensiveOldSmallPropertyLow() {
        Property property = new Property("2 Old Rd", 900_000, 1, 1, 600, 1950);

        double score = scoringService.score(property);

        assertThat(score).isLessThan(40);
    }

    @Test
    void clampsScoreWithinZeroToHundred() {
        Property cheap = new Property("3 Bargain Ave", 10_000, 5, 5, 3000, 2026);
        Property pricey = new Property("4 Luxury Blvd", 10_000_000, 0, 0, 500, 1900);

        assertThat(scoringService.score(cheap)).isBetween(0.0, 100.0);
        assertThat(scoringService.score(pricey)).isBetween(0.0, 100.0);
    }
}
