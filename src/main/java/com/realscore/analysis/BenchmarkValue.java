package com.realscore.analysis;

import java.math.BigDecimal;

/**
 * One median with how it was obtained.
 *
 * @param benchmarkLevel the level the median was taken at: the locality's own, or a coarser one when the locality
 *                       alone had too few listings
 * @param median rounded to whole CZK
 * @param sampleSize how many listings the median is based on
 */
public record BenchmarkValue(LocalityLevel benchmarkLevel, BigDecimal median, int sampleSize) {
}
