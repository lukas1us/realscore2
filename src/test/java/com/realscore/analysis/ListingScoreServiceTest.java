package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.parsing.ListingDetailsQueryService;
import com.realscore.parsing.ListingOverview;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Records snapshots through {@link ListingSnapshotService}, which parses them automatically (TASK-33), then scores
 * them with the default scoring settings. The Postgres container is shared with other tests, so each test uses its
 * own region and only looks at scores in it.
 */
@SpringBootTest(properties = {
        "realscore.analysis.min-sample-size=2",
        "realscore.analysis.large-cities=Testov"})
@Import(PostgresContainerConfig.class)
class ListingScoreServiceTest {

    private static final String SOURCE = "score-test";
    private static final int SALE = 1;
    private static final int RENT = 2;
    private static final int FLATS = 1;
    private static final int HOUSES = 2;
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingScoreService listingScoreService;

    @Autowired
    private ListingDetailsQueryService listingDetailsQueryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void scoresOnlyActiveSaleFlatsBestFirst() {
        String region = "Kraj score-only-sales";
        String cheap = record(region, SALE, FLATS, 5_000_000, 50);
        String pricey = record(region, SALE, FLATS, 7_000_000, 50);
        markRemoved(record(region, SALE, FLATS, 6_000_000, 50));
        record(region, SALE, HOUSES, 6_000_000, 50);
        record(region, SALE, FLATS, 6_000_000, null);
        String rental = record(region, RENT, FLATS, 20_000, 50);

        assertThat(scores(region)).extracting(ListingScore::externalId).containsExactly(cheap, pricey)
                .doesNotContain(rental);
    }

    @Test
    void combinesDiscountRentalYieldAndNoDrop() {
        String region = "Kraj score-parts";
        String cheap = record(region, SALE, FLATS, 5_000_000, 50);
        record(region, SALE, FLATS, 7_000_000, 50);
        record(region, RENT, FLATS, 20_000, 50);
        record(region, RENT, FLATS, 30_000, 60);

        ListingScore score = score(region, cheap);

        assertThat(score.discount().rawPercent()).isEqualByComparingTo("16.67");
        assertThat(score.discount().benchmark()).isEqualTo(
                new BenchmarkValue(LocalityLevel.CITY_PART, new BigDecimal("120000"), 2));
        assertThat(score.rentalYield().rawPercent()).isEqualByComparingTo("5.40");
        assertThat(score.rentalYield().benchmark()).isEqualTo(
                new BenchmarkValue(LocalityLevel.CITY_PART, new BigDecimal("450"), 2));
        assertThat(score.priceDrop().rawPercent()).isEqualByComparingTo("0");
        assertThat(score.priceDrop().benchmark()).isNull();
        assertThat(score.quality().score()).isEqualByComparingTo("50.0");
        assertThat(score.quality().weightPercent()).isEqualByComparingTo("15.0");
        assertThat(score.quality().factors()).extracting(QualityFactor::points).containsOnly(0);
        assertThat(score.total()).isEqualByComparingTo("59.5");
        assertThat(score.city()).isEqualTo("Testov");
        assertThat(score.priceCzk()).isEqualByComparingTo("5000000");
    }

    @Test
    void usesTheMonthlyRentMedianWhenNoRentPerM2MedianApplies() {
        String region = "Kraj score-monthly-rent";
        String cheap = record(region, SALE, FLATS, 5_000_000, 50);
        record(region, SALE, FLATS, 7_000_000, 50);
        record(region, RENT, FLATS, 20_000, null);
        record(region, RENT, FLATS, 30_000, null);

        ListingScore score = score(region, cheap);

        assertThat(score.rentalYield().rawPercent()).isEqualByComparingTo("6.00");
        assertThat(score.rentalYield().benchmark().median()).isEqualByComparingTo("25000");
    }

    @Test
    void includesARecentPriceDrop() {
        String region = "Kraj score-drop";
        String dropped = record(region, SALE, FLATS, 5_000_000, 50);
        record(region, SALE, FLATS, 7_000_000, 50);
        rerecord(dropped, region, SALE, FLATS, 4_500_000, 50);

        ListingScore score = score(region, dropped);

        assertThat(score.priceDrop().rawPercent()).isEqualByComparingTo("10.00");
        assertThat(score.priceDrop().score()).isEqualByComparingTo("66.7");
        assertThat(score.rentalYield()).isNull();
    }

    @Test
    void addsTheQualityFactorsOfTheFlatAndShowsEachOne() {
        String region = "Kraj score-quality";
        String good = record(region, SALE, FLATS, 6_000_000, 50, """
                "ownership": {"name": "Osobní", "value": 1},
                "building_type": {"name": "Cihlová", "value": 2},
                "energy_efficiency_rating_cb": {"name": "C - Úsporná", "value": 3},
                "building_condition": {"name": "Po rekonstrukci", "value": 9},
                "floor_number": 5,
                "elevator": {"name": "Ne", "value": 2},
                """);
        String plain = record(region, SALE, FLATS, 6_000_000, 50);

        ListingScore score = score(region, good);

        assertThat(score.quality().factors()).containsExactly(
                new QualityFactor("ownership", "OV", 10),
                new QualityFactor("construction", "Cihlová", 10),
                new QualityFactor("energyClass", "C", 3),
                new QualityFactor("buildingCondition", "Po rekonstrukci", 10),
                new QualityFactor("elevator", "no, floor 5", -10));
        assertThat(score.quality().points()).isEqualTo(23);
        assertThat(score.quality().score()).isEqualByComparingTo("73.0");
        assertThat(scores(region)).extracting(ListingScore::externalId).containsExactly(good, plain);
    }

    @Test
    void anImplausibleSaleIsNotScored() {
        String region = "Kraj score-implausible";
        record(region, SALE, FLATS, 5_000_000, 50);
        record(region, SALE, FLATS, 7_000_000, 50);
        record(region, RENT, FLATS, 20_000, 50);
        record(region, RENT, FLATS, 30_000, 60);
        String rentAsSale = record(region, SALE, FLATS, 12_500, 56);

        assertThat(scores(region)).extracting(ListingScore::externalId).hasSize(2).doesNotContain(rentAsSale);
        ListingOverview listing = listingDetailsQueryService.findListing(SOURCE, rentAsSale).orElseThrow();
        assertThat(listingScoreService.score(listing)).isEmpty();
    }

    @Test
    void aSaleOutOfScopeIsNotScored() {
        String region = "Kraj score-out-of-scope";
        record(region, SALE, FLATS, 5_000_000, 50);
        record(region, SALE, FLATS, 7_000_000, 50);
        String aboveCap = record(region, SALE, FLATS, 5_210_000, 50);
        listingSnapshotService.markOutOfScope(SOURCE, List.of(aboveCap), Instant.now());

        assertThat(scores(region)).extracting(ListingScore::externalId).hasSize(2).doesNotContain(aboveCap);
        ListingOverview listing = listingDetailsQueryService.findListing(SOURCE, aboveCap).orElseThrow();
        assertThat(listing.outOfScopeSince()).isNotNull();
        assertThat(listingScoreService.score(listing)).isEmpty();
    }

    @Test
    void aFlatWithoutAnyBenchmarkIsNotScored() {
        String region = "Kraj score-alone";
        record(region, SALE, FLATS, 5_000_000, 50);

        assertThat(scores(region)).isEmpty();
    }

    private List<ListingScore> scores(String region) {
        return listingScoreService.scoreAll().stream().filter(s -> region.equals(s.region())).toList();
    }

    private ListingScore score(String region, String externalId) {
        return scores(region).stream().filter(s -> s.externalId().equals(externalId)).findFirst().orElseThrow();
    }

    /** Records a 2+kk in Testov's city part Střed (a large city, so grouped by city part); returns its ID. */
    private String record(String region, int transactionType, int category, long price, Integer size) {
        return record(region, transactionType, category, price, size, "");
    }

    /** @param extraFields JSON members, each followed by a comma, added to the detail's result */
    private String record(String region, int transactionType, int category, long price, Integer size,
            String extraFields) {
        String externalId = String.valueOf(NEXT_ID.incrementAndGet());
        rerecord(externalId, region, transactionType, category, price, size, extraFields);
        return externalId;
    }

    private void rerecord(String externalId, String region, int transactionType, int category, long price,
            Integer size) {
        rerecord(externalId, region, transactionType, category, price, size, "");
    }

    private void rerecord(String externalId, String region, int transactionType, int category, long price,
            Integer size, String extraFields) {
        String payload = """
                {"result": {
                  "price_summary_czk": %d,
                  %s
                  %s
                  "category_sub_cb": {"name": "2+kk", "value": 4},
                  "category_type_cb": {"name": "x", "value": %d},
                  "category_main_cb": {"name": "x", "value": %d},
                  "locality": {"region": "%s", "district": "Testov-okres", "city": "Testov", "citypart": "Střed"}
                }}
                """.formatted(price, size == null ? "" : "\"usable_area\": " + size + ",", extraFields,
                transactionType, category, region);
        listingSnapshotService.recordFetch(SOURCE, externalId, payload);
    }

    private void markRemoved(String externalId) {
        jdbc.update("UPDATE listing SET removed_at = now() WHERE source = ? AND external_id = ?", SOURCE, externalId);
    }
}
