package com.realscore.analysis;

import com.realscore.analysis.internal.Locality;
import com.realscore.analysis.internal.MedianBenchmarks.Benchmark;
import com.realscore.analysis.internal.MedianBenchmarks.Index;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Sale and rent benchmarks computed once (by {@link MarketBenchmarkService#benchmarks()}), for looking up the ones
 * that apply to any number of flats. A snapshot: listings recorded later are not reflected.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class MarketBenchmarks {

    private final Index salePricePerM2;
    private final Index monthlyRent;
    private final Index rentPerM2;
    private final Set<String> largeCities;

    /**
     * Groups the flat like the benchmark endpoints do: by city part in a large city, otherwise by city, falling back
     * to district or region when finer fields are missing.
     *
     * @return all {@code null} benchmarks when the flat has no disposition or no region
     */
    public ListingBenchmarks forListing(String region, String district, String city, String citypart,
            Integer dispositionCode) {
        Locality locality = Locality.finest(region, district, city, citypart, largeCities);
        if (locality == null || dispositionCode == null) {
            return ListingBenchmarks.NONE;
        }
        return new ListingBenchmarks(find(salePricePerM2, locality, dispositionCode),
                find(monthlyRent, locality, dispositionCode), find(rentPerM2, locality, dispositionCode));
    }

    private static BenchmarkValue find(Index index, Locality locality, int dispositionCode) {
        return index.find(locality, dispositionCode).map(MarketBenchmarks::value).orElse(null);
    }

    static BenchmarkValue value(Benchmark b) {
        return new BenchmarkValue(b.benchmarkLocality().level(), b.median(), b.sampleSize());
    }
}
