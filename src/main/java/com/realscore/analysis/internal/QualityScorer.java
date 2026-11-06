package com.realscore.analysis.internal;

import com.realscore.analysis.QualityFactor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Scores the flat itself, as configured by {@link ScoringProperties.Quality}: 50 plus the points of each factor,
 * clamped to 0-100. A missing factor, or a value without configured points, adds 0, so a listing is never penalised
 * for what it does not state.
 */
@Component
@RequiredArgsConstructor
public class QualityScorer {

    private static final int BASE = 50;
    private static final int MIN = 0;
    private static final int MAX = 100;

    private final ScoringProperties properties;

    /**
     * Parsed values the factors come from; any may be {@code null}.
     *
     * @param ownership {@code OV}, {@code DV} or {@code OTHER}
     * @param constructionType Sreality building type label, e.g. {@code Cihlová}
     * @param energyClass {@code A} to {@code G}
     * @param buildingConditionCode Sreality {@code building_condition} value
     * @param floor Sreality {@code floor_number}: podlaží (NP), 1 is the ground floor, below 1 underground or unclear
     * @param elevator {@code false} only for an explicit "Ne"
     */
    public record Factors(String ownership, String constructionType, String energyClass,
            Integer buildingConditionCode, String buildingConditionLabel, Integer floor, Boolean elevator) {
    }

    /** @param score 0-100, rounded to 1 decimal */
    public record Result(BigDecimal score, int points, List<QualityFactor> factors) {
    }

    public Result score(Factors f) {
        ScoringProperties.Quality q = properties.quality();
        List<QualityFactor> factors = List.of(
                new QualityFactor("ownership", f.ownership(), ownership(q.ownership(), f.ownership())),
                new QualityFactor("construction", f.constructionType(),
                        construction(q.construction(), f.constructionType())),
                new QualityFactor("energyClass", f.energyClass(), energyClass(q.energyClass(), f.energyClass())),
                new QualityFactor("buildingCondition", f.buildingConditionLabel(),
                        buildingCondition(q.buildingCondition(), f.buildingConditionCode())),
                new QualityFactor("elevator", elevatorValue(f), elevator(q.elevator(), f)));
        int points = factors.stream().mapToInt(QualityFactor::points).sum();
        int score = Math.clamp(BASE + points, MIN, MAX);
        return new Result(BigDecimal.valueOf(score).setScale(1, RoundingMode.UNNECESSARY), points, factors);
    }

    private static int ownership(ScoringProperties.Ownership points, String ownership) {
        if (ownership == null) {
            return 0;
        }
        return switch (ownership) {
            case "OV" -> points.personal();
            case "DV" -> points.cooperative();
            default -> 0;
        };
    }

    private static int construction(ScoringProperties.Construction points, String constructionType) {
        if (constructionType == null) {
            return 0;
        }
        return switch (constructionType) {
            case "Cihlová" -> points.brick();
            case "Panelová" -> points.panel();
            default -> 0;
        };
    }

    private static int energyClass(ScoringProperties.EnergyClass points, String energyClass) {
        if (energyClass == null) {
            return 0;
        }
        return switch (energyClass) {
            case "A" -> points.a();
            case "B" -> points.b();
            case "C" -> points.c();
            case "D" -> points.d();
            case "E" -> points.e();
            case "F" -> points.f();
            case "G" -> points.g();
            default -> 0;
        };
    }

    /** Codes are Sreality's building_condition values (doc "Sreality API"); 7 (K demolici) and others add 0. */
    private static int buildingCondition(ScoringProperties.BuildingCondition points, Integer code) {
        if (code == null) {
            return 0;
        }
        return switch (code) {
            case 1 -> points.veryGood();
            case 2 -> points.good();
            case 3 -> points.poor();
            case 4 -> points.underConstruction();
            case 5 -> points.project();
            case 6 -> points.newBuild();
            case 8 -> points.beforeReconstruction();
            case 9 -> points.afterReconstruction();
            case 10 -> points.inReconstruction();
            default -> 0;
        };
    }

    /** Only an elevator explicitly marked as missing counts, and only with a known floor high enough. */
    private static int elevator(ScoringProperties.Elevator points, Factors f) {
        boolean missingHighUp = Boolean.FALSE.equals(f.elevator()) && f.floor() != null
                && f.floor() >= points.fromFloor();
        return missingHighUp ? points.missing() : 0;
    }

    /** E.g. {@code no, floor 5}; {@code null} when the elevator is unknown. */
    private static String elevatorValue(Factors f) {
        if (f.elevator() == null) {
            return null;
        }
        String value = Boolean.TRUE.equals(f.elevator()) ? "yes" : "no";
        return f.floor() == null ? value : value + ", floor " + f.floor();
    }
}
