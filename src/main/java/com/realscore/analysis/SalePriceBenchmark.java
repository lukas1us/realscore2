package com.realscore.analysis;

import java.math.BigDecimal;

/**
 * The median asking price per m² of active flats for sale with one disposition in one locality.
 *
 * @param region the locality's path; fields finer than the locality are {@code null}
 * @param benchmarkLevel the level the median was taken at: the locality's own, or a coarser one when the locality
 *                       alone had too few listings
 * @param medianPricePerM2Czk rounded to whole CZK
 * @param sampleSize how many listings the median is based on
 */
public record SalePriceBenchmark(
        String region,
        String district,
        String city,
        String cityPart,
        int dispositionCode,
        String disposition,
        LocalityLevel benchmarkLevel,
        BigDecimal medianPricePerM2Czk,
        int sampleSize) {
}
