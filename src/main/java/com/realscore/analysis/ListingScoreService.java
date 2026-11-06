package com.realscore.analysis;

import com.realscore.analysis.internal.AnalysisProperties;
import com.realscore.analysis.internal.PlausibilityProperties;
import com.realscore.analysis.internal.QualityScorer;
import com.realscore.analysis.internal.ScoreCalculator;
import com.realscore.parsing.ActiveListingDetails;
import com.realscore.parsing.ActivePriceHistory;
import com.realscore.parsing.ListingDetails;
import com.realscore.parsing.ListingDetailsQueryService;
import com.realscore.parsing.ListingOverview;
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
 * m², gross rental yield from the local rent medians, the price drop over the price-drop window, and the quality of
 * the flat itself (ownership, construction, energy class, building condition, elevator). Only flats with
 * a price, a positive size, a disposition and a region are scored, and only if a sale or rent benchmark applies.
 * Flats whose price or size is implausible ({@link PlausibilityProperties}) are not scored: their discount and yield
 * would be meaningless and push them to the top.
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
    private final QualityScorer qualityScorer;
    private final AnalysisProperties properties;
    private final PlausibilityProperties plausibility;

    private record Key(String source, String externalId) {
    }

    /** The fields a score needs, from either a list of active listings or a single listing. */
    private record Candidate(String source, String externalId, Integer dispositionCode, String dispositionLabel,
            String region, String district, String city, String citypart, String url, BigDecimal priceCzk,
            BigDecimal sizeM2, QualityScorer.Factors quality) {

        static Candidate of(ActiveListingDetails d) {
            return new Candidate(d.source(), d.externalId(), d.dispositionCode(), d.dispositionLabel(), d.region(),
                    d.district(), d.city(), d.citypart(), d.url(), d.priceCzk(), d.sizeM2(),
                    new QualityScorer.Factors(d.ownership(), d.constructionType(), d.energyClass(),
                            d.buildingConditionCode(), d.buildingConditionLabel(), d.floor(), d.elevator()));
        }

        static Candidate of(ListingOverview listing) {
            ListingDetails d = listing.details();
            return new Candidate(listing.source(), listing.externalId(), d.dispositionCode(), d.dispositionLabel(),
                    d.region(), d.district(), d.city(), d.citypart(), d.url(), d.priceCzk(), d.sizeM2(),
                    new QualityScorer.Factors(d.ownership(), d.constructionType(), d.energyClass(),
                            d.buildingConditionCode(), d.buildingConditionLabel(), d.floor(), d.elevator()));
        }

        boolean isScorable() {
            return priceCzk != null && priceCzk.signum() > 0 && sizeM2 != null && sizeM2.signum() > 0
                    && dispositionCode != null && region != null;
        }
    }

    /** @return every scorable flat, best first */
    public List<ListingScore> scoreAll() {
        MarketBenchmarks benchmarks = marketBenchmarkService.benchmarks();
        Map<Key, PriceDrop> drops = priceDropService.find(properties.priceDropDays(), TransactionType.SALE).stream()
                .collect(Collectors.toMap(d -> new Key(d.source(), d.externalId()), Function.identity()));
        return listingDetails.findActive(TransactionType.SALE.code(), MarketBenchmarkService.FLATS).stream()
                .map(Candidate::of)
                .filter(this::isScorable)
                .map(c -> score(c, benchmarks, drops.get(new Key(c.source(), c.externalId()))))
                .flatMap(Optional::stream)
                .sorted(BEST_FIRST)
                .toList();
    }

    /**
     * Each filter is optional ({@code null}) and matches exactly, ignoring case.
     *
     * @param disposition label such as {@code 2+kk}
     * @return the best {@code limit} scored flats that match the filters, best first
     */
    public List<ListingScore> top(int limit, String region, String city, String disposition) {
        return scoreAll().stream()
                .filter(s -> matches(region, s.region()) && matches(city, s.city())
                        && matches(disposition, s.dispositionLabel()))
                .limit(limit)
                .toList();
    }

    /**
     * Scores one listing like {@link #scoreAll()} would, with its drop taken from its own price history.
     *
     * @return empty unless the listing is an active, in-scope, scorable flat for sale
     */
    public Optional<ListingScore> score(ListingOverview listing) {
        ListingDetails d = listing.details();
        if (listing.removedAt() != null || listing.outOfScopeSince() != null || d == null
                || !Integer.valueOf(TransactionType.SALE.code()).equals(d.transactionTypeCode())
                || !Integer.valueOf(MarketBenchmarkService.FLATS).equals(d.propertyCategoryCode())) {
            return Optional.empty();
        }
        Candidate candidate = Candidate.of(listing);
        if (!isScorable(candidate)) {
            return Optional.empty();
        }
        ActivePriceHistory history = new ActivePriceHistory(listing.source(), listing.externalId(),
                d.transactionTypeCode(), d.dispositionLabel(), d.sizeM2(), d.city(), d.citypart(), d.url(),
                listing.priceHistory());
        PriceDrop drop = PriceDrop.of(history, PriceDropService.windowStart(properties.priceDropDays())).orElse(null);
        return score(candidate, marketBenchmarkService.benchmarks(), drop);
    }

    private boolean isScorable(Candidate c) {
        return c.isScorable() && plausibility.sale().isPlausible(c.priceCzk(), c.sizeM2());
    }

    private static boolean matches(String filter, String value) {
        return filter == null || filter.equalsIgnoreCase(value);
    }

    private Optional<ListingScore> score(Candidate c, MarketBenchmarks benchmarks, PriceDrop drop) {
        ListingBenchmarks b = benchmarks.forListing(c.region(), c.district(), c.city(), c.citypart(),
                c.dispositionCode());
        BenchmarkValue saleBenchmark = b.salePricePerM2();
        BenchmarkValue rentBenchmark = b.rentPerM2() != null ? b.rentPerM2() : b.monthlyRent();

        BigDecimal discount = saleBenchmark == null ? null : discountPercent(c, saleBenchmark.median());
        BigDecimal rentalYield = rentBenchmark == null ? null : rentalYieldPercent(c, expectedRent(c, b));
        BigDecimal priceDrop = drop == null ? BigDecimal.ZERO : drop.dropPercent();
        QualityScorer.Result quality = qualityScorer.score(c.quality());

        return calculator.score(discount, rentalYield, priceDrop, quality.score()).map(r -> new ListingScore(
                c.source(), c.externalId(), c.dispositionLabel(), c.region(), c.district(), c.city(), c.citypart(),
                c.url(), c.priceCzk(), c.sizeM2(), r.total(), part(r.discount(), saleBenchmark),
                part(r.rentalYield(), rentBenchmark), part(r.priceDrop(), null),
                new QualityScorePart(r.quality().score(), r.quality().weightPercent(), quality.points(),
                        quality.factors())));
    }

    /** (median − listing price per m²) / median, in percent: positive when cheaper than the market. */
    private static BigDecimal discountPercent(Candidate c, BigDecimal medianPerM2) {
        BigDecimal perM2 = c.priceCzk().divide(c.sizeM2(), DIVISION_SCALE, RoundingMode.HALF_UP);
        return medianPerM2.subtract(perM2).multiply(HUNDRED).divide(medianPerM2, DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    /** Rent per m² median × size, or the monthly rent median when no per-m² median applies. */
    private static BigDecimal expectedRent(Candidate c, ListingBenchmarks b) {
        return b.rentPerM2() != null ? b.rentPerM2().median().multiply(c.sizeM2()) : b.monthlyRent().median();
    }

    /** Expected monthly rent × 12 / price, in percent. */
    private static BigDecimal rentalYieldPercent(Candidate c, BigDecimal monthlyRent) {
        return monthlyRent.multiply(MONTHS).multiply(HUNDRED)
                .divide(c.priceCzk(), DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    private static ScorePart part(ScoreCalculator.Part part, BenchmarkValue benchmark) {
        return part == null ? null : new ScorePart(part.rawPercent(), part.score(), part.weightPercent(), benchmark);
    }
}
