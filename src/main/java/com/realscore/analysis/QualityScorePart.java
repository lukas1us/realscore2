package com.realscore.analysis;

import java.math.BigDecimal;
import java.util.List;

/**
 * The quality part of a listing's score: 50 plus the points of its factors, clamped to 0-100.
 *
 * @param score 0-100, rounded to 1 decimal
 * @param weightPercent this part's share of the total in percent, after rescaling for missing parts
 * @param points sum of the factors' points
 * @param factors every factor, also those the listing does not have (0 points)
 */
public record QualityScorePart(BigDecimal score, BigDecimal weightPercent, int points, List<QualityFactor> factors) {
}
