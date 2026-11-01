package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * Records snapshots through {@link ListingSnapshotService}, which parses them into listing_detail (TASK-33), then
 * reads the benchmarks. The Postgres container is shared with other tests, so each test uses its own region and only
 * looks at benchmarks in it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "realscore.analysis.min-sample-size=2",
        "realscore.analysis.large-cities=Testov"})
@Import(PostgresContainerConfig.class)
class MarketBenchmarkServiceTest {

    private static final String SOURCE = "benchmark-test";
    private static final int SALE = 1;
    private static final int RENT = 2;
    private static final int FLATS = 1;
    private static final int HOUSES = 2;
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    @LocalServerPort
    int port;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private MarketBenchmarkService marketBenchmarkService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void saleMedianIsOverActiveSaleFlatsWithPriceAndSize() {
        String region = "Kraj sale-median";
        record(region, "Střed", SALE, FLATS, 5_000_000, 50);
        record(region, "Střed", SALE, FLATS, 6_000_000, 50);
        record(region, "Střed", SALE, FLATS, 7_000_000, 50);
        markRemoved(record(region, "Střed", SALE, FLATS, 90_000_000, 50));
        record(region, "Střed", RENT, FLATS, 90_000_000, 50);
        record(region, "Střed", SALE, HOUSES, 90_000_000, 50);
        record(region, "Střed", SALE, FLATS, 0, 50);
        record(region, "Střed", SALE, FLATS, 90_000_000, null);

        assertThat(salePriceBenchmarks(region)).containsExactly(new SalePriceBenchmark(region, "Testov-okres",
                "Testov", "Střed", 4, "2+kk", LocalityLevel.CITY_PART, new BigDecimal("120000"), 3));
    }

    @Test
    void aCityPartWithTooFewListingsFallsBackToTheWholeCity() {
        String region = "Kraj sale-fallback";
        record(region, "Střed", SALE, FLATS, 5_000_000, 50);
        record(region, "Sever", SALE, FLATS, 7_000_000, 50);

        assertThat(salePriceBenchmarks(region))
                .extracting(SalePriceBenchmark::cityPart, SalePriceBenchmark::benchmarkLevel,
                        SalePriceBenchmark::medianPricePerM2Czk, SalePriceBenchmark::sampleSize)
                .containsExactly(
                        tuple("Sever", LocalityLevel.CITY, new BigDecimal("120000"), 2),
                        tuple("Střed", LocalityLevel.CITY, new BigDecimal("120000"), 2));
    }

    @Test
    void anEmptyCityPartCountsInTheCityAndGetsNoCityPartBenchmarkOfItsOwn() {
        String region = "Kraj sale-empty-citypart";
        record(region, "Střed", SALE, FLATS, 5_000_000, 50);
        record(region, "", SALE, FLATS, 7_000_000, 50);
        record(region, "Sever", SALE, FLATS, 6_000_000, 50);
        record(region, "Sever", SALE, FLATS, 6_000_000, 50);

        assertThat(salePriceBenchmarks(region))
                .extracting(SalePriceBenchmark::cityPart, SalePriceBenchmark::benchmarkLevel,
                        SalePriceBenchmark::medianPricePerM2Czk, SalePriceBenchmark::sampleSize)
                .containsExactly(
                        tuple(null, LocalityLevel.CITY, new BigDecimal("120000"), 4),
                        tuple("Sever", LocalityLevel.CITY_PART, new BigDecimal("120000"), 2),
                        tuple("Střed", LocalityLevel.CITY, new BigDecimal("120000"), 4));
    }

    @Test
    void endpointReturnsTheSaleBenchmarks() {
        String region = "Kraj sale-endpoint";
        record(region, "Střed", SALE, FLATS, 5_000_000, 50);
        record(region, "Střed", SALE, FLATS, 7_000_000, 50);

        List<SalePriceBenchmark> response = RestClient.create("http://localhost:" + port).get()
                .uri("/api/analysis/benchmarks/sale-price-per-m2")
                .retrieve()
                .body(new ParameterizedTypeReference<List<SalePriceBenchmark>>() { });

        assertThat(response).filteredOn(b -> region.equals(b.region())).isEqualTo(salePriceBenchmarks(region));
        assertThat(salePriceBenchmarks(region)).hasSize(1);
    }

    @Test
    void rentMediansAreOverActiveRentalFlatsAndSalesAreNotCounted() {
        String region = "Kraj rent-median";
        record(region, "Střed", RENT, FLATS, 20_000, 50);
        record(region, "Střed", RENT, FLATS, 30_000, 60);
        record(region, "Střed", RENT, FLATS, 40_000, null);
        markRemoved(record(region, "Střed", RENT, FLATS, 900_000, 50));
        record(region, "Střed", SALE, FLATS, 900_000, 50);
        record(region, "Střed", RENT, HOUSES, 900_000, 50);
        record(region, "Střed", RENT, FLATS, 0, 50);

        assertThat(rentBenchmarks(region)).containsExactly(new RentBenchmark(region, "Testov-okres", "Testov",
                "Střed", 4, "2+kk",
                new BenchmarkValue(LocalityLevel.CITY_PART, new BigDecimal("30000"), 3),
                new BenchmarkValue(LocalityLevel.CITY_PART, new BigDecimal("450"), 2)));
    }

    @Test
    void rentPerM2IsLeftEmptyWhenTooFewRentalsHaveASize() {
        String region = "Kraj rent-no-size";
        record(region, "Střed", RENT, FLATS, 20_000, null);
        record(region, "Střed", RENT, FLATS, 30_000, 60);

        assertThat(rentBenchmarks(region)).singleElement().satisfies(b -> {
            assertThat(b.monthlyRentCzk()).isEqualTo(
                    new BenchmarkValue(LocalityLevel.CITY_PART, new BigDecimal("25000"), 2));
            assertThat(b.rentPerM2Czk()).isNull();
        });
    }

    @Test
    void endpointReturnsTheRentBenchmarks() {
        String region = "Kraj rent-endpoint";
        record(region, "Střed", RENT, FLATS, 20_000, 50);
        record(region, "Střed", RENT, FLATS, 30_000, 60);

        List<RentBenchmark> response = RestClient.create("http://localhost:" + port).get()
                .uri("/api/analysis/benchmarks/rent")
                .retrieve()
                .body(new ParameterizedTypeReference<List<RentBenchmark>>() { });

        assertThat(response).filteredOn(b -> region.equals(b.region())).isEqualTo(rentBenchmarks(region));
        assertThat(rentBenchmarks(region)).hasSize(1);
    }

    @Test
    void forListingGivesTheSameMediansAsTheBenchmarkEndpoints() {
        String region = "Kraj lookup-same";
        record(region, "Střed", SALE, FLATS, 5_000_000, 50);
        record(region, "Sever", SALE, FLATS, 7_000_000, 50);
        record(region, "Střed", RENT, FLATS, 20_000, 50);
        record(region, "Střed", RENT, FLATS, 30_000, 60);

        ListingBenchmarks benchmarks = marketBenchmarkService.benchmarks()
                .forListing(region, "Testov-okres", "Testov", "Střed", 4);

        SalePriceBenchmark sale = salePriceBenchmarks(region).stream()
                .filter(b -> "Střed".equals(b.cityPart())).findFirst().orElseThrow();
        RentBenchmark rent = rentBenchmarks(region).getFirst();
        assertThat(benchmarks.salePricePerM2()).isEqualTo(
                new BenchmarkValue(sale.benchmarkLevel(), sale.medianPricePerM2Czk(), sale.sampleSize()));
        assertThat(benchmarks.monthlyRent()).isEqualTo(rent.monthlyRentCzk());
        assertThat(benchmarks.rentPerM2()).isEqualTo(rent.rentPerM2Czk());
        assertThat(benchmarks.salePricePerM2().benchmarkLevel()).isEqualTo(LocalityLevel.CITY);
    }

    @Test
    void forListingFallsBackForALocalityWithoutRentalsOfItsOwn() {
        String region = "Kraj lookup-fallback";
        record(region, "Střed", RENT, FLATS, 20_000, 50);
        record(region, "Střed", RENT, FLATS, 30_000, 60);

        ListingBenchmarks benchmarks = marketBenchmarkService.benchmarks()
                .forListing(region, "Testov-okres", "Testov", "Jih", 4);

        assertThat(benchmarks.monthlyRent()).isEqualTo(
                new BenchmarkValue(LocalityLevel.CITY, new BigDecimal("25000"), 2));
        assertThat(benchmarks.salePricePerM2()).isNull();
    }

    @Test
    void forListingWithoutDispositionLocalityOrAnyBenchmarkGivesNone() {
        MarketBenchmarks benchmarks = marketBenchmarkService.benchmarks();

        assertThat(benchmarks.forListing("Kraj lookup-none", "Testov-okres", "Testov", "Střed", 4))
                .isEqualTo(new ListingBenchmarks(null, null, null));
        assertThat(benchmarks.forListing(null, "Testov-okres", "Testov", "Střed", 4))
                .isEqualTo(new ListingBenchmarks(null, null, null));
        assertThat(benchmarks.forListing("Kraj lookup-none", "Testov-okres", "Testov", "Střed", null))
                .isEqualTo(new ListingBenchmarks(null, null, null));
    }

    private List<RentBenchmark> rentBenchmarks(String region) {
        return marketBenchmarkService.rent().stream().filter(b -> region.equals(b.region())).toList();
    }

    private List<SalePriceBenchmark> salePriceBenchmarks(String region) {
        return marketBenchmarkService.salePricePerM2().stream().filter(b -> region.equals(b.region())).toList();
    }

    /** Records a 2+kk in Testov (a large city, so grouped by city part) and returns its external ID. */
    private String record(String region, String cityPart, int transactionType, int category, long price,
            Integer size) {
        String externalId = String.valueOf(NEXT_ID.incrementAndGet());
        String payload = """
                {"result": {
                  "price_summary_czk": %d,
                  %s
                  "category_sub_cb": {"name": "2+kk", "value": 4},
                  "category_type_cb": {"name": "x", "value": %d},
                  "category_main_cb": {"name": "x", "value": %d},
                  "locality": {"region": "%s", "district": "Testov-okres", "city": "Testov", "citypart": "%s"}
                }}
                """.formatted(price, size == null ? "" : "\"usable_area\": " + size + ",", transactionType, category,
                region, cityPart);
        listingSnapshotService.recordFetch(SOURCE, externalId, payload);
        return externalId;
    }

    private void markRemoved(String externalId) {
        jdbc.update("UPDATE listing SET removed_at = now() WHERE source = ? AND external_id = ?", SOURCE, externalId);
    }
}
