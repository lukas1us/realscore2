package com.realscore.analysis.internal;

import static com.realscore.analysis.internal.DefaultScoringProperties.defaults;
import static com.realscore.analysis.internal.DefaultScoringProperties.part;
import static com.realscore.analysis.internal.DefaultScoringProperties.withDiscount;
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
        assertThat(validator.validate(defaults())).isEmpty();
    }

    @Test
    void maxMustBeAboveMinAndWeightNotNegative() {
        assertThat(validator.validate(withDiscount(part("30", "30", "50"))))
                .extracting(ConstraintViolation::getMessage).containsExactly("max must be greater than min");
        assertThat(validator.validate(withDiscount(part("-30", "30", "-1"))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("discount.weight");
    }

    @Test
    void qualityWeightMustNotBeNegativeAndEveryPointsValueIsRequired() {
        ScoringProperties d = defaults();
        ScoringProperties.Quality q = d.quality();
        ScoringProperties negativeWeight = new ScoringProperties(d.discount(), d.rentalYield(), d.priceDrop(),
                new ScoringProperties.Quality(new BigDecimal("-1"), q.ownership(), q.construction(),
                        q.energyClass(), q.buildingCondition(), q.elevator()));
        ScoringProperties missingPoints = new ScoringProperties(d.discount(), d.rentalYield(), d.priceDrop(),
                new ScoringProperties.Quality(q.weight(), new ScoringProperties.Ownership(null, -10),
                        q.construction(), q.energyClass(), q.buildingCondition(), q.elevator()));

        assertThat(validator.validate(negativeWeight))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("quality.weight");
        assertThat(validator.validate(missingPoints))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("quality.ownership.personal");
    }
}
