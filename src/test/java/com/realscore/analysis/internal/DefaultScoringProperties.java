package com.realscore.analysis.internal;

import java.math.BigDecimal;

/** The default scoring settings from {@code application.properties}, for unit tests without Spring. */
final class DefaultScoringProperties {

    private DefaultScoringProperties() {
    }

    static ScoringProperties defaults() {
        return withDiscount(part("-30", "30", "45"));
    }

    static ScoringProperties withDiscount(ScoringProperties.Part discount) {
        return new ScoringProperties(discount, part("2", "8", "30"), part("0", "15", "10"), quality("15"));
    }

    static ScoringProperties.Quality quality(String weight) {
        return new ScoringProperties.Quality(new BigDecimal(weight),
                new ScoringProperties.Ownership(10, -10),
                new ScoringProperties.Construction(10, -5),
                new ScoringProperties.EnergyClass(10, 7, 3, 0, -3, -7, 0),
                new ScoringProperties.BuildingCondition(10, 10, 5, 0, -10, -20, 0, 0, 0),
                new ScoringProperties.Elevator(5, -10));
    }

    static ScoringProperties.Part part(String min, String max, String weight) {
        return new ScoringProperties.Part(new BigDecimal(min), new BigDecimal(max), new BigDecimal(weight));
    }
}
