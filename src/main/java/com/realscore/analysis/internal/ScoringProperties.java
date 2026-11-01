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
 */
@Validated
@ConfigurationProperties("realscore.analysis.scoring")
public record ScoringProperties(@NotNull @Valid Part discount, @NotNull @Valid Part rentalYield,
        @NotNull @Valid Part priceDrop) {

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
}
