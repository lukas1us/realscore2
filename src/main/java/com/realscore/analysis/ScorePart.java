package com.realscore.analysis;

import java.math.BigDecimal;

/**
 * One part of a listing's score.
 *
 * @param rawPercent the measured value in percent (discount, gross yield or drop), rounded to 2 decimals
 * @param score 0-100, rounded to 1 decimal
 * @param weightPercent this part's share of the total in percent, after rescaling for missing parts
 * @param benchmark the median the raw value was computed from; {@code null} for the price drop
 */
public record ScorePart(BigDecimal rawPercent, BigDecimal score, BigDecimal weightPercent, BenchmarkValue benchmark) {
}
