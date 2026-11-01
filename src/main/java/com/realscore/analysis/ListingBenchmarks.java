package com.realscore.analysis;

/**
 * The benchmarks that apply to one flat: those of its locality and disposition, or of an enclosing locality when its
 * own has too few listings. Each is {@code null} when there is none, even at region level.
 *
 * @param salePricePerM2 median asking price per m² of active flats for sale
 * @param monthlyRent median monthly rent of active flats for rent
 * @param rentPerM2 median monthly rent per m² of active flats for rent; its fallback is independent of
 *                  {@code monthlyRent}'s
 */
public record ListingBenchmarks(BenchmarkValue salePricePerM2, BenchmarkValue monthlyRent, BenchmarkValue rentPerM2) {

    static final ListingBenchmarks NONE = new ListingBenchmarks(null, null, null);
}
