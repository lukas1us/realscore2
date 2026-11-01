package com.realscore.analysis.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the market benchmarks and price drops; values live in {@code application.properties} under {@code realscore.analysis}.
 *
 * @param largeCities cities (as Sreality names them, e.g. {@code Praha}) grouped by city part instead of as a whole
 * @param minSampleSize fewest listings a group needs for its median to be used; smaller groups fall back to the
 *                      enclosing locality
 * @param priceDropDays default length in days of the window over which price drops are listed
 */
@Validated
@ConfigurationProperties("realscore.analysis")
public record AnalysisProperties(@NotNull Set<String> largeCities, @Positive int minSampleSize,
        @Positive int priceDropDays) {
}
