package com.realscore.analysis.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bounds outside which a listing's price or size is taken to be a seller's mistake or placeholder (a rent listed as a
 * sale, a 1 CZK rent, a sale price listed as rent) rather than a real offer. Such listings are left out of the
 * benchmarks and get no score. Values live in {@code application.properties} under
 * {@code realscore.analysis.plausibility}; they are applied on demand to the parsed details, so a change takes effect
 * at once, without a reparse.
 *
 * @param sale bounds for flats for sale, prices in CZK
 * @param rent bounds for flats for rent, prices in CZK per month
 */
@Validated
@ConfigurationProperties("realscore.analysis.plausibility")
public record PlausibilityProperties(@NotNull @Valid Bounds sale, @NotNull @Valid Bounds rent) {

    /**
     * Every bound is inclusive and optional: {@code null} (an empty value) means no bound. A bound applies only to a
     * value the listing has, so a listing without a size is judged on its price alone.
     *
     * @param minPricePerM2 lowest price per m² of usable area; with {@code maxPricePerM2} it catches a price and a
     *                      size that don't fit each other
     */
    public record Bounds(@PositiveOrZero BigDecimal minPrice, @PositiveOrZero BigDecimal maxPrice,
            @PositiveOrZero BigDecimal minPricePerM2, @PositiveOrZero BigDecimal maxPricePerM2,
            @PositiveOrZero BigDecimal minSize, @PositiveOrZero BigDecimal maxSize) {

        @AssertTrue(message = "each max must not be below its min")
        public boolean isRangeValid() {
            return ordered(minPrice, maxPrice) && ordered(minPricePerM2, maxPricePerM2) && ordered(minSize, maxSize);
        }

        /** @param priceCzk {@code null} when unknown @param sizeM2 {@code null} when unknown */
        public boolean isPlausible(BigDecimal priceCzk, BigDecimal sizeM2) {
            boolean hasSize = sizeM2 != null && sizeM2.signum() > 0;
            if (priceCzk != null && !within(priceCzk, minPrice, maxPrice)) {
                return false;
            }
            if (hasSize && !within(sizeM2, minSize, maxSize)) {
                return false;
            }
            return priceCzk == null || !hasSize
                    || within(priceCzk, multiply(minPricePerM2, sizeM2), multiply(maxPricePerM2, sizeM2));
        }

        private static BigDecimal multiply(BigDecimal perM2, BigDecimal sizeM2) {
            return perM2 == null ? null : perM2.multiply(sizeM2);
        }

        private static boolean within(BigDecimal value, BigDecimal min, BigDecimal max) {
            return (min == null || value.compareTo(min) >= 0) && (max == null || value.compareTo(max) <= 0);
        }

        private static boolean ordered(BigDecimal min, BigDecimal max) {
            return min == null || max == null || min.compareTo(max) <= 0;
        }
    }
}
