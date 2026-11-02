package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the whole bulk path against WireMock and a real Postgres. Region 1 holds every collected ID; every other
 * region, and the rental search unless a test stubs it, is empty.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class SrealityBulkCollectorTest {

    private static final long PRICE_CAP = 5_000_000;
    private static final Long NO_RENT_PRICE_CAP = null;
    private static final String SMALL_REGION = "1";
    /** 4004 is recorded just before the run, so it is never due. */
    private static final Duration REFETCH_AFTER = Duration.ofDays(7);

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void sreality(DynamicPropertyRegistry registry) {
        registry.add("realscore.sreality.base-url", sreality::baseUrl);
        registry.add("realscore.sreality.request-delay", () -> "0ms");
        registry.add("realscore.sreality.initial-backoff", () -> "1ms");
    }

    @Autowired
    private SrealityBulkCollector bulkCollector;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void persistsNewEstatesSkipsGoneOnesRetriesThrottledOnesAndLeavesKnownOnesAlone() {
        stubSearch("search_page_bulk.json");
        String detail = fixture("detail_success.json");
        listingSnapshotService.recordFetch("sreality", "4004", detail);

        stubDetail(4001, okJson(detail));
        stubDetail(4002, aResponse().withStatus(404));
        sreality.stubFor(get(urlEqualTo(detailPath(4003))).inScenario("throttled")
                .whenScenarioStateIs(STARTED).willReturn(aResponse().withStatus(429)).willSetStateTo("ok"));
        sreality.stubFor(get(urlEqualTo(detailPath(4003))).inScenario("throttled")
                .whenScenarioStateIs("ok").willReturn(okJson(detail)));

        BulkCollectionResult result = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(result).isEqualTo(new BulkCollectionResult(Set.of(4001L, 4002L, 4003L, 4004L), 1, 2, 0, 0, 0, 1, 0));
        assertThat(snapshotCount(4001)).isEqualTo(1);
        assertThat(snapshotCount(4003)).isEqualTo(1);
        assertThat(listingCount(4002)).isZero();
        assertThat(snapshotCount(4004)).isEqualTo(1);
        sreality.verify(2, getRequestedFor(urlEqualTo(detailPath(4003))));
        sreality.verify(0, getRequestedFor(urlEqualTo(detailPath(4004))));
    }

    @Test
    void persistentFailureForOneEstateIsCountedAndTheRestIsStillFetched() {
        stubSearch("search_page_bulk_with_failure.json");
        stubDetail(4101, aResponse().withStatus(500));
        stubDetail(4102, okJson(fixture("detail_success.json")));

        BulkCollectionResult result = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(result).isEqualTo(new BulkCollectionResult(Set.of(4101L, 4102L), 0, 1, 0, 0, 0, 0, 1));
        assertThat(listingCount(4101)).isZero();
        assertThat(snapshotCount(4102)).isEqualTo(1);
    }

    @Test
    void persistsRentalApartmentsAlongsideSaleApartments() {
        stubSearch("search_page_scrape_job.json");
        stubRentalSearch("search_page_rentals.json");
        String detail = fixture("detail_success.json");
        for (long estateId : new long[] {4201, 4202, 4601, 4602}) {
            stubDetail(estateId, okJson(detail));
        }

        BulkCollectionResult result = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(result).isEqualTo(new BulkCollectionResult(Set.of(4201L, 4202L, 4601L, 4602L), 0, 4, 0, 0, 0, 0, 0));
        assertThat(snapshotCount(4601)).isEqualTo(1);
        assertThat(snapshotCount(4602)).isEqualTo(1);
    }

    @Test
    void aKnownListingWithoutAStoredSearchPriceIsNotRefetchedButGetsOne() {
        listingSnapshotService.recordFetch("sreality", "4911", fixture("detail_success.json"));
        stubSearchPage(4911, "3490000.0");

        BulkCollectionResult result = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(result.priceChanged()).isZero();
        sreality.verify(0, getRequestedFor(urlEqualTo(detailPath(4911))));
        assertThat(searchPrice(4911)).isEqualByComparingTo("3490000");
    }

    @Test
    void anUnchangedSearchPriceIsNotRefetched() {
        listingSnapshotService.recordFetch("sreality", "4912", fixture("detail_success.json"));
        setSearchPrice(4912, "3590000");
        stubSearchPage(4912, "3590000.0");

        BulkCollectionResult result = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(result.priceChanged()).isZero();
        sreality.verify(0, getRequestedFor(urlEqualTo(detailPath(4912))));
    }

    @Test
    void aSwitchToPriceOnRequestCountsAsAChange() {
        listingSnapshotService.recordFetch("sreality", "4913", fixture("detail_success.json"));
        setSearchPrice(4913, "3590000");
        stubSearchPage(4913, "0.0");
        stubDetail(4913, okJson(fixture("detail_success.json")));

        BulkCollectionResult result = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(result.priceChanged()).isEqualTo(1);
        assertThat(result.refetched()).isEqualTo(1);
        assertThat(searchPrice(4913)).isEqualByComparingTo("0");
    }

    @Test
    void aFailedRefetchKeepsTheOldSearchPriceSoTheNextRunRetries() {
        listingSnapshotService.recordFetch("sreality", "4914", fixture("detail_success.json"));
        setSearchPrice(4914, "3590000");
        stubSearchPage(4914, "3490000.0");
        stubDetail(4914, aResponse().withStatus(500));

        BulkCollectionResult failedRun = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(failedRun.priceChanged()).isEqualTo(1);
        assertThat(failedRun.failed()).isEqualTo(1);
        assertThat(searchPrice(4914)).isEqualByComparingTo("3590000");

        stubDetail(4914, okJson(fixture("detail_success.json")));
        BulkCollectionResult nextRun = bulkCollector.collect(PRICE_CAP, NO_RENT_PRICE_CAP, REFETCH_AFTER);

        assertThat(nextRun.priceChanged()).isEqualTo(1);
        assertThat(nextRun.refetched()).isEqualTo(1);
        assertThat(searchPrice(4914)).isEqualByComparingTo("3490000");
    }

    /** Region 1 of the sale search answers with one estate at the given {@code price_summary_czk}. */
    private static void stubSearchPage(long estateId, String price) {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH)).atPriority(10)
                .willReturn(okJson(fixture("search_count_empty.json"))));
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("category_type_cb", equalTo("1"))
                .withQueryParam("locality_region_id", equalTo(SMALL_REGION))
                .willReturn(okJson("""
                        {"pagination": {"limit": 100, "offset": 0, "total": 1},
                         "results": [{"hash_id": %d, "price_summary_czk": %s}]}
                        """.formatted(estateId, price))));
    }

    private void setSearchPrice(long estateId, String price) {
        jdbc.update("UPDATE listing SET search_price_czk = ? WHERE source = 'sreality' AND external_id = ?",
                new BigDecimal(price), String.valueOf(estateId));
    }

    private BigDecimal searchPrice(long estateId) {
        return jdbc.queryForObject("SELECT search_price_czk FROM listing WHERE source = 'sreality' AND external_id = ?",
                BigDecimal.class, String.valueOf(estateId));
    }

    /** Region 1 of the sale search answers with {@code pageFixture}; everything else is empty. */
    private static void stubSearch(String pageFixture) {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH)).atPriority(10)
                .willReturn(okJson(fixture("search_count_empty.json"))));
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("category_type_cb", equalTo("1"))
                .withQueryParam("locality_region_id", equalTo(SMALL_REGION))
                .willReturn(okJson(fixture(pageFixture))));
    }

    /** Region 1 of the rental search answers with {@code pageFixture}. */
    private static void stubRentalSearch(String pageFixture) {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("category_type_cb", equalTo("2"))
                .withQueryParam("locality_region_id", equalTo(SMALL_REGION))
                .willReturn(okJson(fixture(pageFixture))));
    }

    private static void stubDetail(long estateId, ResponseDefinitionBuilder response) {
        sreality.stubFor(get(urlEqualTo(detailPath(estateId))).willReturn(response));
    }

    private static String detailPath(long estateId) {
        return "/api/v1/estates/" + estateId;
    }

    private static String fixture(String name) {
        String path = "fixtures/sreality/" + name;
        try (InputStream in = SrealityBulkCollectorTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int listingCount(long estateId) {
        return jdbc.queryForObject("SELECT count(*) FROM listing WHERE source = 'sreality' AND external_id = ?",
                Integer.class, String.valueOf(estateId));
    }

    private int snapshotCount(long estateId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM listing_snapshot s JOIN listing l ON l.id = s.listing_id
                WHERE l.source = 'sreality' AND l.external_id = ?
                """, Integer.class, String.valueOf(estateId));
    }
}
