package com.realscore.analysis.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns a listing's raw percentages into sub-scores and a total, as configured by {@link ScoringProperties}. Each
 * part maps linearly to 0-100 between its {@code min} and {@code max}, clamped; the total is the weighted average of
 * the parts that have data, so the weights of the others are shared among them.
 */
@Component
@RequiredArgsConstructor
public class ScoreCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int DIVISION_SCALE = 10;

    private final ScoringProperties properties;

    /**
     * @param rawPercent rounded to 2 decimals
     * @param score 0-100, rounded to 1 decimal
     * @param weightPercent this part's share of the total in percent after rescaling, rounded to 1 decimal
     */
    public record Part(BigDecimal rawPercent, BigDecimal score, BigDecimal weightPercent) {
    }

    /** @param total 0-100, rounded to 1 decimal; a part without data is {@code null} */
    public record Result(BigDecimal total, Part discount, Part rentalYield, Part priceDrop) {
    }

    private record Input(BigDecimal raw, ScoringProperties.Part config) {
    }

    /**
     * @param discountPercent {@code null} without a sale benchmark
     * @param rentalYieldPercent {@code null} without a rent benchmark
     * @param priceDropPercent never {@code null}: no drop is 0
     * @return empty when neither the discount nor the rental yield is known, since a drop alone says nothing about
     *     the price, or when the known parts all weigh 0
     */
    public Optional<Result> score(BigDecimal discountPercent, BigDecimal rentalYieldPercent,
            BigDecimal priceDropPercent) {
        if (discountPercent == null && rentalYieldPercent == null) {
            return Optional.empty();
        }
        List<Input> known = new ArrayList<>();
        Input discount = input(discountPercent, properties.discount(), known);
        Input rentalYield = input(rentalYieldPercent, properties.rentalYield(), known);
        Input priceDrop = input(priceDropPercent, properties.priceDrop(), known);
        BigDecimal totalWeight = known.stream().map(i -> i.config().weight()).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() == 0) {
            return Optional.empty();
        }

        Part discountPart = part(discount, totalWeight);
        Part rentalYieldPart = part(rentalYield, totalWeight);
        Part priceDropPart = part(priceDrop, totalWeight);
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (Input input : known) {
            weightedSum = weightedSum.add(subScore(input).multiply(input.config().weight()));
        }
        BigDecimal total = weightedSum.divide(totalWeight, 1, RoundingMode.HALF_UP);
        return Optional.of(new Result(total, discountPart, rentalYieldPart, priceDropPart));
    }

    private static Input input(BigDecimal raw, ScoringProperties.Part config, List<Input> known) {
        if (raw == null) {
            return null;
        }
        Input input = new Input(raw, config);
        known.add(input);
        return input;
    }

    private static Part part(Input input, BigDecimal totalWeight) {
        if (input == null) {
            return null;
        }
        return new Part(input.raw().setScale(2, RoundingMode.HALF_UP), subScore(input),
                input.config().weight().multiply(HUNDRED).divide(totalWeight, 1, RoundingMode.HALF_UP));
    }

    /** Rounded to 1 decimal, so the total can be recomputed from the sub-scores shown. */
    private static BigDecimal subScore(Input input) {
        BigDecimal min = input.config().min();
        BigDecimal max = input.config().max();
        BigDecimal linear = input.raw().subtract(min).multiply(HUNDRED)
                .divide(max.subtract(min), DIVISION_SCALE, RoundingMode.HALF_UP);
        BigDecimal clamped = linear.max(BigDecimal.ZERO).min(HUNDRED);
        return clamped.setScale(1, RoundingMode.HALF_UP);
    }
}
