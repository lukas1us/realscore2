package com.realscore.analysis.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.analysis.internal.PlausibilityProperties.Bounds;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Uses the default bounds from {@code application.properties}. */
class PlausibilityPropertiesTest {

    static final PlausibilityProperties DEFAULTS = new PlausibilityProperties(
            bounds("200000", null, "5000", "400000", "12", "400"),
            bounds("1000", "300000", "50", "2000", "12", "400"));

    private static final Bounds SALE = DEFAULTS.sale();
    private static final Bounds RENT = DEFAULTS.rent();

    static Bounds bounds(String minPrice, String maxPrice, String minPerM2, String maxPerM2, String minSize,
            String maxSize) {
        return new Bounds(decimal(minPrice), decimal(maxPrice), decimal(minPerM2), decimal(maxPerM2),
                decimal(minSize), decimal(maxSize));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static boolean plausible(Bounds bounds, String price, String size) {
        return bounds.isPlausible(decimal(price), decimal(size));
    }

    /** Each pair sits on both sides of one threshold; the other values are ordinary. */
    @ParameterizedTest(name = "sale {0} CZK, {1} m² -> {2}")
    @CsvSource(nullValues = "null", value = {
            "200000, 20, true", "199999, 20, false",          // min price (10,000/m²)
            "250000, 50, true", "249950, 50, false",          // min 5,000/m²
            "4800000, 12, true", "4800012, 12, false",        // max 400,000/m²
            "2000000, 12, true", "2000000, 11.9, false",      // min size
            "4000000, 400, true", "4000000, 401, false",      // max size
            "90000000, null, true",                           // no max price; without a size only the price counts
    })
    void saleBounds(String price, String size, boolean plausible) {
        assertThat(plausible(SALE, price, size)).isEqualTo(plausible);
    }

    @ParameterizedTest(name = "rent {0} CZK, {1} m² -> {2}")
    @CsvSource(nullValues = "null", value = {
            "1000, 12, true", "999, 12, false",               // min price (83/m²)
            "300000, 150, true", "300001, 150, false",        // max price (2,000/m²)
            "2500, 50, true", "2499, 50, false",              // min 50/m²
            "60000, 30, true", "60001, 30, false",            // max 2,000/m²
            "8000, 12, true", "8000, 11, false",              // min size
            "200000, 400, true", "200000, 401, false",        // max size
            "300000, null, true", "300001, null, false",      // without a size only the price counts
    })
    void rentBounds(String price, String size, boolean plausible) {
        assertThat(plausible(RENT, price, size)).isEqualTo(plausible);
    }

    /** The examples in TASK-45 and an ordinary listing of each kind. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "sale priced as a monthly rent, true, 12500, 56, false",
            "'sale at 3,363 CZK/m²', true, 134500, 40, false",
            "1+kk sale of 9 m², true, 2500000, 9, false",
            "ordinary sale, true, 3490000, 56, true",
            "1 CZK rent, false, 1, 53, false",
            "300 CZK rent, false, 300, 48, false",
            "sale price listed as a rent, false, 6893000, 57, false",
            "'ordinary rent of 5,731 m²', false, 17000, 5731, false",
            "ordinary rent, false, 16000, 55, true",
            "Prague luxury rent, false, 175000, 187, true",
    })
    void examples(String description, boolean sale, String price, String size, boolean plausible) {
        assertThat(plausible(sale ? SALE : RENT, price, size)).as(description).isEqualTo(plausible);
    }

    @Test
    void missingPriceAndSizeAreNotImplausible() {
        assertThat(plausible(SALE, null, null)).isTrue();
        assertThat(plausible(RENT, null, "50")).isTrue();
    }

    @Test
    void defaultsAreValidAndAMaxBelowItsMinIsNot() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(DEFAULTS)).isEmpty();
        assertThat(validator.validate(bounds(null, null, null, null, "50", "40"))).hasSize(1);
    }

    @Nested
    @SpringBootTest
    @Import(PostgresContainerConfig.class)
    class Configured {

        @Autowired
        private PlausibilityProperties properties;

        @Test
        void applicationPropertiesHoldTheDefaultsTestedHere() {
            assertThat(properties).usingRecursiveComparison().withComparatorForType(BigDecimal::compareTo,
                    BigDecimal.class).isEqualTo(DEFAULTS);
        }
    }
}
