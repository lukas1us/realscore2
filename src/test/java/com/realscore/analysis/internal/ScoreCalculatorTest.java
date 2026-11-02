package com.realscore.analysis.internal;

import static com.realscore.analysis.internal.DefaultScoringProperties.part;
import static com.realscore.analysis.internal.DefaultScoringProperties.quality;
import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.analysis.internal.ScoreCalculator.Result;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Uses the default limits and weights from {@code application.properties}. */
class ScoreCalculatorTest {

    private static final String NEUTRAL_QUALITY = "50";

    private final ScoreCalculator calculator = new ScoreCalculator(DefaultScoringProperties.defaults());

    @Test
    void eachPartMapsLinearlyBetweenItsLimits() {
        Result result = score("0", "5", "7.5", NEUTRAL_QUALITY);

        assertThat(result.discount().score()).isEqualByComparingTo("50.0");
        assertThat(result.rentalYield().score()).isEqualByComparingTo("50.0");
        assertThat(result.priceDrop().score()).isEqualByComparingTo("50.0");
        assertThat(result.quality().score()).isEqualByComparingTo("50.0");
        assertThat(result.total()).isEqualByComparingTo("50.0");
    }

    @Test
    void scoresAreClampedAtBothEnds() {
        Result high = score("45", "12", "20", "100");
        Result low = score("-40", "1", "0", "0");

        assertThat(high.discount().score()).isEqualByComparingTo("100");
        assertThat(high.rentalYield().score()).isEqualByComparingTo("100");
        assertThat(high.priceDrop().score()).isEqualByComparingTo("100");
        assertThat(high.total()).isEqualByComparingTo("100");
        assertThat(low.discount().score()).isEqualByComparingTo("0");
        assertThat(low.rentalYield().score()).isEqualByComparingTo("0");
        assertThat(low.total()).isEqualByComparingTo("0");
    }

    @Test
    void theTotalIsTheWeightedAverage() {
        Result result = score("30", "2", "0", NEUTRAL_QUALITY);

        assertThat(result.total()).isEqualByComparingTo("52.5");
        assertThat(result.discount().weightPercent()).isEqualByComparingTo("45.0");
        assertThat(result.rentalYield().weightPercent()).isEqualByComparingTo("30.0");
        assertThat(result.priceDrop().weightPercent()).isEqualByComparingTo("10.0");
        assertThat(result.quality().weightPercent()).isEqualByComparingTo("15.0");
    }

    @Test
    void theQualityScoreCountsWithItsWeightAndHasNoRawPercent() {
        Result result = score("0", "5", "7.5", "80");

        assertThat(result.quality().rawPercent()).isNull();
        assertThat(result.quality().score()).isEqualByComparingTo("80");
        assertThat(result.total()).isEqualByComparingTo("54.5");
    }

    @Test
    void aMissingPartIsLeftOutAndTheOtherWeightsAreRescaled() {
        Result result = score("0", null, "0", NEUTRAL_QUALITY);

        assertThat(result.rentalYield()).isNull();
        assertThat(result.discount().weightPercent()).isEqualByComparingTo("64.3");
        assertThat(result.priceDrop().weightPercent()).isEqualByComparingTo("14.3");
        assertThat(result.quality().weightPercent()).isEqualByComparingTo("21.4");
        assertThat(result.total()).isEqualByComparingTo("42.9");
    }

    @Test
    void aDropAndQualityAloneAreNotScored() {
        assertThat(calculator.score(null, null, new BigDecimal("10"), new BigDecimal("90"))).isEmpty();
    }

    @Test
    void rawValuesAreRoundedForDisplayButScoredUnrounded() {
        Result result = score("16.666666", "5.4", "0", NEUTRAL_QUALITY);

        assertThat(result.discount().rawPercent()).isEqualByComparingTo("16.67");
        assertThat(result.discount().score()).isEqualByComparingTo("77.8");
        assertThat(result.rentalYield().score()).isEqualByComparingTo("56.7");
        assertThat(result.total()).isEqualByComparingTo("59.5");
    }

    @Test
    void knownPartsThatAllWeighNothingAreNotScored() {
        ScoreCalculator unweighted = new ScoreCalculator(new ScoringProperties(
                part("-30", "30", "0"), part("2", "8", "0"), part("0", "15", "0"), quality("0")));

        assertThat(unweighted.score(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN)).isEmpty();
    }

    private Result score(String discount, String rentalYield, String priceDrop, String quality) {
        return calculator.score(decimal(discount), decimal(rentalYield), decimal(priceDrop), decimal(quality))
                .orElseThrow();
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
