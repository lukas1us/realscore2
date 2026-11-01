package com.realscore.analysis;

/**
 * The median asking rent of active flats for rent with one disposition in one locality.
 *
 * @param region the locality's path; fields finer than the locality are {@code null}
 * @param monthlyRentCzk median monthly rent, over listings with a price
 * @param rentPerM2Czk median monthly rent per m², over those listings that also have a size; {@code null} when too
 *                     few do, even at region level. Its fallback is independent of {@code monthlyRentCzk}'s, so the
 *                     two can come from different levels
 */
public record RentBenchmark(
        String region,
        String district,
        String city,
        String cityPart,
        int dispositionCode,
        String disposition,
        BenchmarkValue monthlyRentCzk,
        BenchmarkValue rentPerM2Czk) {
}
