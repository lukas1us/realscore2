package com.realscore.analysis.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the listing score; values live in {@code application.properties} under
 * {@code realscore.analysis.scoring}.
 *
 * @param discount how much cheaper per m² than the local sale median, in percent
 * @param rentalYield gross yearly rent from the local rent medians over the price, in percent
 * @param priceDrop percent drop over the price-drop window
 * @param quality points per quality factor of the flat itself
 */
@Validated
@ConfigurationProperties("realscore.analysis.scoring")
public record ScoringProperties(@NotNull @Valid Part discount, @NotNull @Valid Part rentalYield,
        @NotNull @Valid Part priceDrop, @NotNull @Valid Quality quality) {

    /**
     * @param min raw percentage that scores 0; anything lower scores 0 too
     * @param max raw percentage that scores 100; anything higher scores 100 too
     * @param weight relative weight of the part in the total
     */
    public record Part(@NotNull BigDecimal min, @NotNull BigDecimal max, @NotNull @PositiveOrZero BigDecimal weight) {

        @AssertTrue(message = "max must be greater than min")
        public boolean isRangeValid() {
            return min == null || max == null || max.compareTo(min) > 0;
        }
    }

    /**
     * The quality part starts at 50 and each factor adds its points (negative ones subtract); the sum is clamped to
     * 0-100. A factor that is missing, or has a value not listed here, adds 0.
     *
     * @param weight relative weight of the part in the total
     */
    public record Quality(@NotNull @PositiveOrZero BigDecimal weight, @NotNull @Valid Ownership ownership,
            @NotNull @Valid Construction construction, @NotNull @Valid EnergyClass energyClass,
            @NotNull @Valid BuildingCondition buildingCondition, @NotNull @Valid Elevator elevator) {
    }

    /** @param personal osobní vlastnictví (OV) @param cooperative družstevní (DV) */
    public record Ownership(@NotNull Integer personal, @NotNull Integer cooperative) {
    }

    /** @param brick Cihlová @param panel Panelová */
    public record Construction(@NotNull Integer brick, @NotNull Integer panel) {
    }

    public record EnergyClass(@NotNull Integer a, @NotNull Integer b, @NotNull Integer c, @NotNull Integer d,
            @NotNull Integer e, @NotNull Integer f, @NotNull Integer g) {
    }

    /** One value per Sreality building condition, named after its label (e.g. newBuild is Novostavba). */
    public record BuildingCondition(@NotNull Integer newBuild, @NotNull Integer afterReconstruction,
            @NotNull Integer veryGood, @NotNull Integer good, @NotNull Integer beforeReconstruction,
            @NotNull Integer poor, @NotNull Integer underConstruction, @NotNull Integer project,
            @NotNull Integer inReconstruction) {
    }

    /**
     * @param fromFloor lowest floor (0 is the ground floor) at which a missing elevator counts
     * @param missing points for an elevator explicitly marked as missing at or above {@code fromFloor}
     */
    public record Elevator(@NotNull Integer fromFloor, @NotNull Integer missing) {
    }
}
