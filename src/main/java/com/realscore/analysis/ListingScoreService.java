package com.realscore.analysis;

import com.realscore.analysis.internal.AnalysisProperties;
import com.realscore.analysis.internal.ScoreCalculator;
import com.realscore.parsing.ActiveListingDetails;
import com.realscore.parsing.ListingDetailsQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Scores active flats for sale against the market, computed on demand: discount against the local sale median per
 * m², gross rental yield from the local rent medians, and the price drop over the price-drop window. Only flats with
 * a price, a positive size, a disposition and a region are scored, and only if a sale or rent benchmark applies.
 */
@Service
@RequiredArgsConstructor
public class ListingScoreService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MONTHS = BigDecimal.valueOf(12);
    private static final int DIVISION_SCALE = 10;
    private static final Comparator<ListingScore> BEST_FIRST = Comparator
            .comparing(ListingScore::total, Comparator.reverseOrder())
            .thenComparing(ListingScore::source)
            .thenComparing(ListingScore::externalId);

    private final ListingDetailsQueryService listingDetails;
    private final MarketBenchmarkService marketBenchmarkService;
    private final PriceDropService priceDropService;
    private final ScoreCalculator calculator;
    private final AnalysisProperties properties;

    private record Key(String source, String externalId) {
    }

    /** @return every scorable flat, best first */
    public List<ListingScore> scoreAll() {
        MarketBenchmarks benchmarks = marketBenchmarkService.benchmarks();
        Map<Key, PriceDrop> drops = priceDropService.find(properties.priceDropDays(), TransactionType.SALE).stream()
                .collect(Collectors.toMap(d -> new Key(d.source(), d.externalId()), Function.identity()));
        return listingDetails.findActive(TransactionType.SALE.code(), MarketBenchmarkService.FLATS).stream()
                .filter(ListingScoreService::isScorable)
                .map(d -> score(d, benchmarks, drops.get(new Key(d.source(), d.externalId()))))
                .flatMap(Optional::stream)
                .sorted(BEST_FIRST)
                .toList();
    }

    private static boolean isScorable(ActiveListingDetails d) {
        return d.priceCzk() != null && d.priceCzk().signum() > 0 && d.sizeM2() != null && d.sizeM2().signum() > 0
                && d.dispositionCode() != null && d.region() != null;
    }

    private Optional<ListingScore> score(ActiveListingDetails d, MarketBenchmarks benchmarks, PriceDrop drop) {
        ListingBenchmarks b = benchmarks.forListing(d.region(), d.district(), d.city(), d.citypart(),
                d.dispositionCode());
        BenchmarkValue saleBenchmark = b.salePricePerM2();
        BenchmarkValue rentBenchmark = b.rentPerM2() != null ? b.rentPerM2() : b.monthlyRent();

        BigDecimal discount = saleBenchmark == null ? null : discountPercent(d, saleBenchmark.median());
        BigDecimal rentalYield = rentBenchmark == null ? null : rentalYieldPercent(d, expectedRent(d, b));
        BigDecimal priceDrop = drop == null ? BigDecimal.ZERO : drop.dropPercent();

        return calculator.score(discount, rentalYield, priceDrop).map(r -> new ListingScore(d.source(),
                d.externalId(), d.dispositionLabel(), d.region(), d.district(), d.city(), d.citypart(), d.priceCzk(),
                d.sizeM2(), r.total(), part(r.discount(), saleBenchmark), part(r.rentalYield(), rentBenchmark),
                part(r.priceDrop(), null)));
    }

    /** (median − listing price per m²) / median, in percent: positive when cheaper than the market. */
    private static BigDecimal discountPercent(ActiveListingDetails d, BigDecimal medianPerM2) {
        BigDecimal perM2 = d.priceCzk().divide(d.sizeM2(), DIVISION_SCALE, RoundingMode.HALF_UP);
        return medianPerM2.subtract(perM2).multiply(HUNDRED).divide(medianPerM2, DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    /** Rent per m² median × size, or the monthly rent median when no per-m² median applies. */
    private static BigDecimal expectedRent(ActiveListingDetails d, ListingBenchmarks b) {
        return b.rentPerM2() != null ? b.rentPerM2().median().multiply(d.sizeM2()) : b.monthlyRent().median();
    }

    /** Expected monthly rent × 12 / price, in percent. */
    private static BigDecimal rentalYieldPercent(ActiveListingDetails d, BigDecimal monthlyRent) {
        return monthlyRent.multiply(MONTHS).multiply(HUNDRED)
                .divide(d.priceCzk(), DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    private static ScorePart part(ScoreCalculator.Part part, BenchmarkValue benchmark) {
        return part == null ? null : new ScorePart(part.rawPercent(), part.score(), part.weightPercent(), benchmark);
    }
}
