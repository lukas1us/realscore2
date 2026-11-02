package com.realscore.analysis;

/**
 * One factor of a listing's quality score.
 *
 * @param factor {@code ownership}, {@code construction}, {@code energyClass}, {@code buildingCondition} or
 *               {@code elevator}
 * @param value the parsed value the points come from, {@code null} when the listing does not have it
 * @param points added to the quality score (negative ones subtract); 0 for a missing or unlisted value
 */
public record QualityFactor(String factor, String value, int points) {
}
