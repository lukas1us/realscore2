package com.realscore.analysis.internal;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoringPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultsAreValid() {
        assertThat(validator.validate(properties(part("-30", "30", "50")))).isEmpty();
    }

    @Test
    void maxMustBeAboveMinAndWeightNotNegative() {
        assertThat(validator.validate(properties(part("30", "30", "50"))))
                .extracting(ConstraintViolation::getMessage).containsExactly("max must be greater than min");
        assertThat(validator.validate(properties(part("-30", "30", "-1"))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("discount.weight");
    }

    private static ScoringProperties properties(ScoringProperties.Part discount) {
        return new ScoringProperties(discount, part("2", "8", "35"), part("0", "15", "15"));
    }

    private static ScoringProperties.Part part(String min, String max, String weight) {
        return new ScoringProperties.Part(new BigDecimal(min), new BigDecimal(max), new BigDecimal(weight));
    }
}
