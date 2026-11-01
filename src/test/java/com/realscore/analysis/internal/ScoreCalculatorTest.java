package com.realscore.analysis.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.analysis.internal.ScoreCalculator.Result;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Uses the default limits and weights from {@code application.properties}. */
class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator(new ScoringProperties(
            part("-30", "30", "50"), part("2", "8", "35"), part("0", "15", "15")));

    @Test
    void eachPartMapsLinearlyBetweenItsLimits() {
        Result result = score("0", "5", "7.5");

        assertThat(result.discount().score()).isEqualByComparingTo("50.0");
        assertThat(result.rentalYield().score()).isEqualByComparingTo("50.0");
        assertThat(result.priceDrop().score()).isEqualByComparingTo("50.0");
        assertThat(result.total()).isEqualByComparingTo("50.0");
    }

    @Test
    void scoresAreClampedAtBothEnds() {
        Result high = score("45", "12", "20");
        Result low = score("-40", "1", "0");

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
        Result result = score("30", "2", "0");

        assertThat(result.total()).isEqualByComparingTo("50.0");
        assertThat(result.discount().weightPercent()).isEqualByComparingTo("50.0");
        assertThat(result.rentalYield().weightPercent()).isEqualByComparingTo("35.0");
        assertThat(result.priceDrop().weightPercent()).isEqualByComparingTo("15.0");
    }

    @Test
    void aMissingPartIsLeftOutAndTheOtherWeightsAreRescaled() {
        Result result = score("0", null, "0");

        assertThat(result.rentalYield()).isNull();
        assertThat(result.discount().weightPercent()).isEqualByComparingTo("76.9");
        assertThat(result.priceDrop().weightPercent()).isEqualByComparingTo("23.1");
        assertThat(result.total()).isEqualByComparingTo("38.5");
    }

    @Test
    void aDropAloneIsNotScored() {
        assertThat(calculator.score(null, null, new BigDecimal("10"))).isEmpty();
    }

    @Test
    void rawValuesAreRoundedForDisplayButScoredUnrounded() {
        Result result = score("16.666666", "5.4", "0");

        assertThat(result.discount().rawPercent()).isEqualByComparingTo("16.67");
        assertThat(result.discount().score()).isEqualByComparingTo("77.8");
        assertThat(result.rentalYield().score()).isEqualByComparingTo("56.7");
        assertThat(result.total()).isEqualByComparingTo("58.7");
    }

    @Test
    void knownPartsThatAllWeighNothingAreNotScored() {
        ScoreCalculator unweighted = new ScoreCalculator(new ScoringProperties(
                part("-30", "30", "0"), part("2", "8", "0"), part("0", "15", "0")));

        assertThat(unweighted.score(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO)).isEmpty();
    }

    private Result score(String discount, String rentalYield, String priceDrop) {
        return calculator.score(decimal(discount), decimal(rentalYield), decimal(priceDrop)).orElseThrow();
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static ScoringProperties.Part part(String min, String max, String weight) {
        return new ScoringProperties.Part(new BigDecimal(min), new BigDecimal(max), new BigDecimal(weight));
    }
}
