package com.realscore.analysis.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.analysis.QualityFactor;
import com.realscore.analysis.internal.QualityScorer.Factors;
import com.realscore.analysis.internal.QualityScorer.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Uses the default points from {@code application.properties}. */
class QualityScorerTest {

    private static final Factors NOTHING_KNOWN = new Factors(null, null, null, null, null, null, null);

    private final QualityScorer scorer = new QualityScorer(DefaultScoringProperties.defaults());

    @Test
    void aListingWithoutAnyFactorIsNeutralAndListsEveryFactorWithZeroPoints() {
        Result result = scorer.score(NOTHING_KNOWN);

        assertThat(result.score()).isEqualByComparingTo("50.0");
        assertThat(result.points()).isZero();
        assertThat(result.factors()).containsExactly(
                new QualityFactor("ownership", null, 0),
                new QualityFactor("construction", null, 0),
                new QualityFactor("energyClass", null, 0),
                new QualityFactor("buildingCondition", null, 0),
                new QualityFactor("elevator", null, 0));
    }

    @Test
    void addsThePointsOfEveryFactor() {
        Result result = scorer.score(new Factors("OV", "Cihlová", "C", 9, "Po rekonstrukci", 5, false));

        assertThat(result.factors()).containsExactly(
                new QualityFactor("ownership", "OV", 10),
                new QualityFactor("construction", "Cihlová", 10),
                new QualityFactor("energyClass", "C", 3),
                new QualityFactor("buildingCondition", "Po rekonstrukci", 10),
                new QualityFactor("elevator", "no, floor 5", -10));
        assertThat(result.points()).isEqualTo(23);
        assertThat(result.score()).isEqualByComparingTo("73.0");
    }

    @ParameterizedTest
    @CsvSource({"OV, 10", "DV, -10", "OTHER, 0"})
    void ownership(String ownership, int points) {
        assertThat(points(new Factors(ownership, null, null, null, null, null, null), "ownership")).isEqualTo(points);
    }

    @ParameterizedTest
    @CsvSource({"Cihlová, 10", "Panelová, -5", "Smíšená, 0", "Skeletová, 0"})
    void construction(String construction, int points) {
        assertThat(points(new Factors(null, construction, null, null, null, null, null), "construction"))
                .isEqualTo(points);
    }

    @ParameterizedTest
    @CsvSource({"A, 10", "B, 7", "C, 3", "D, 0", "E, -3", "F, -7", "G, 0"})
    void energyClass(String energyClass, int points) {
        assertThat(points(new Factors(null, null, energyClass, null, null, null, null), "energyClass"))
                .isEqualTo(points);
    }

    @ParameterizedTest
    @CsvSource({
            "6, Novostavba, 10",
            "9, Po rekonstrukci, 10",
            "1, Velmi dobrý, 5",
            "2, Dobrý, 0",
            "8, Před rekonstrukcí, -10",
            "3, Špatný, -20",
            "4, Ve výstavbě, 0",
            "5, Projekt, 0",
            "10, V rekonstrukci, 0",
            "7, K demolici, 0",
    })
    void buildingConditionIsMappedByCode(int code, String label, int points) {
        assertThat(points(new Factors(null, null, null, code, label, null, null), "buildingCondition"))
                .isEqualTo(points);
    }

    /** Floors are Sreality podlaží (NP): 1 is the ground floor, 5 (the default threshold) is the 4th floor above it. */
    @ParameterizedTest
    @CsvSource({
            "false, 5, -10",
            "false, 6, -10",
            "false, 4, 0",
            "false, 1, 0",
            "false, 0, 0",
            "false, -1, 0",
            "true, 8, 0",
            ", 8, 0",
            "false, , 0",
    })
    void onlyAnExplicitlyMissingElevatorFromTheFourthFloorAboveGroundCosts(Boolean elevator, Integer floor, int points) {
        assertThat(points(new Factors(null, null, null, null, null, floor, elevator), "elevator")).isEqualTo(points);
    }

    @Test
    void theScoreIsClampedToZeroAndHundred() {
        QualityScorer harsh = new QualityScorer(new ScoringProperties(null, null, null,
                new ScoringProperties.Quality(null, new ScoringProperties.Ownership(60, -60),
                        new ScoringProperties.Construction(0, 0),
                        new ScoringProperties.EnergyClass(0, 0, 0, 0, 0, 0, 0),
                        new ScoringProperties.BuildingCondition(0, 0, 0, 0, 0, 0, 0, 0, 0),
                        new ScoringProperties.Elevator(4, 0))));

        assertThat(harsh.score(new Factors("OV", null, null, null, null, null, null)).score())
                .isEqualByComparingTo("100");
        assertThat(harsh.score(new Factors("DV", null, null, null, null, null, null)).score())
                .isEqualByComparingTo("0");
    }

    private int points(Factors factors, String factor) {
        return scorer.score(factors).factors().stream().filter(f -> f.factor().equals(factor)).findFirst()
                .orElseThrow().points();
    }
}
