package com.realscore.analysis;

import com.realscore.analysis.internal.AnalysisProperties;
import com.realscore.analysis.internal.Locality;
import com.realscore.analysis.internal.MedianBenchmarks;
import com.realscore.analysis.internal.MedianBenchmarks.Benchmark;
import com.realscore.analysis.internal.MedianBenchmarks.Sample;
import com.realscore.parsing.ActiveListingDetails;
import com.realscore.parsing.ListingDetailsQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Market benchmarks per locality and disposition, computed on demand from the parsed details of active listings.
 * Locality grouping and fallback follow {@link MedianBenchmarks}, configured by {@link AnalysisProperties}.
 */
@Service
@RequiredArgsConstructor
public class MarketBenchmarkService {

    static final int SALE = 1;
    static final int RENT = 2;
    static final int FLATS = 1;

    private final ListingDetailsQueryService listingDetails;
    private final AnalysisProperties properties;

    /**
     * Over active flats for sale that have a price, a positive size, a disposition and at least a region. Sales are
     * only scraped below the configured sale price cap, so in expensive areas this is the median of the cheaper end.
     */
    public List<SalePriceBenchmark> salePricePerM2() {
        List<Sample> samples = listingDetails.findActive(SALE, FLATS).stream()
                .filter(d -> d.priceCzk() != null && d.sizeM2() != null && d.sizeM2().signum() > 0)
                .map(d -> sample(d, d.priceCzk().divide(d.sizeM2(), 4, RoundingMode.HALF_UP)))
                .filter(Objects::nonNull)
                .toList();
        return MedianBenchmarks.compute(samples, properties.minSampleSize()).stream()
                .map(MarketBenchmarkService::toSalePriceBenchmark)
                .toList();
    }

    /**
     * Over active flats for rent that have a price, a disposition and at least a region; the per-m² median only over
     * those of them that also have a positive size.
     */
    public List<RentBenchmark> rent() {
        List<ActiveListingDetails> rentals = listingDetails.findActive(RENT, FLATS).stream()
                .filter(d -> d.priceCzk() != null)
                .toList();
        List<Sample> rentSamples = rentals.stream()
                .map(d -> sample(d, d.priceCzk()))
                .filter(Objects::nonNull)
                .toList();
        List<Sample> perM2Samples = rentals.stream()
                .filter(d -> d.sizeM2() != null && d.sizeM2().signum() > 0)
                .map(d -> sample(d, d.priceCzk().divide(d.sizeM2(), 4, RoundingMode.HALF_UP)))
                .filter(Objects::nonNull)
                .toList();
        Map<BenchmarkKey, Benchmark> perM2 = MedianBenchmarks.compute(perM2Samples, properties.minSampleSize())
                .stream()
                .collect(Collectors.toMap(BenchmarkKey::of, b -> b));
        return MedianBenchmarks.compute(rentSamples, properties.minSampleSize()).stream()
                .map(b -> toRentBenchmark(b, perM2.get(BenchmarkKey.of(b))))
                .toList();
    }

    private record BenchmarkKey(Locality locality, int dispositionCode) {
        static BenchmarkKey of(Benchmark b) {
            return new BenchmarkKey(b.locality(), b.dispositionCode());
        }
    }

    /** @return {@code null} when the listing has no disposition or no locality to group it by */
    private Sample sample(ActiveListingDetails details, BigDecimal value) {
        Locality locality = Locality.finest(details.region(), details.district(), details.city(), details.citypart(),
                properties.largeCities());
        if (locality == null || details.dispositionCode() == null) {
            return null;
        }
        return new Sample(locality, details.dispositionCode(), details.dispositionLabel(), value);
    }

    /**
     * Every per-m² sample is also a rent sample, so a per-m² benchmark never exists without a rent one for the same
     * locality and disposition.
     */
    private static RentBenchmark toRentBenchmark(Benchmark rent, Benchmark perM2) {
        Locality l = rent.locality();
        return new RentBenchmark(l.region(), l.district(), l.city(), l.cityPart(), rent.dispositionCode(),
                rent.dispositionLabel(), value(rent), perM2 == null ? null : value(perM2));
    }

    private static BenchmarkValue value(Benchmark b) {
        return new BenchmarkValue(b.benchmarkLocality().level(), b.median(), b.sampleSize());
    }

    private static SalePriceBenchmark toSalePriceBenchmark(Benchmark b) {
        Locality l = b.locality();
        return new SalePriceBenchmark(l.region(), l.district(), l.city(), l.cityPart(), b.dispositionCode(),
                b.dispositionLabel(), b.benchmarkLocality().level(), b.median(), b.sampleSize());
    }
}
