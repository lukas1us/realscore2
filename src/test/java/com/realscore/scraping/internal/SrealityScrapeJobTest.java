package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.scraping.ScrapeRunStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Calls {@link SrealityScrapeJob#run()} directly against WireMock and a real Postgres. The configured cron fires only
 * at midnight on January 1st, so the scheduler never starts a run during the test.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class SrealityScrapeJobTest {

    private static final String TEST_CRON = "0 0 0 1 1 *";

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void sreality(DynamicPropertyRegistry registry) {
        registry.add("realscore.sreality.base-url", sreality::baseUrl);
        registry.add("realscore.sreality.request-delay", () -> "0ms");
        registry.add("realscore.sreality.initial-backoff", () -> "1ms");
        registry.add("realscore.scrape.cron", () -> TEST_CRON);
    }

    @Autowired
    private SrealityScrapeJob job;

    @Autowired
    private List<ScheduledTaskHolder> scheduledTaskHolders;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void jobIsScheduledOnTheConfiguredCron() {
        assertThat(scheduledTaskHolders.stream().flatMap(holder -> holder.getScheduledTasks().stream()))
                .map(ScheduledTask::getTask)
                .filteredOn(CronTask.class::isInstance)
                .map(task -> ((CronTask) task).getExpression())
                .containsExactly(TEST_CRON);
    }

    @Test
    void successfulRunIsClosedWithItsCounts() {
        stubSearch(okJson(fixture("search_page_scrape_job.json")));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4201")).willReturn(okJson(fixture("detail_success.json"))));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4202")).willReturn(aResponse().withStatus(500)));

        job.run();

        Map<String, Object> run = latestRun();
        assertThat(run.get("status")).isEqualTo(ScrapeRunStatus.SUCCEEDED.name());
        assertThat(run.get("finished_at")).isNotNull();
        assertThat(run.get("found_count")).isEqualTo(2);
        assertThat(run.get("new_count")).isEqualTo(1);
        assertThat(run.get("error_count")).isEqualTo(1);
        assertThat(run.get("error_details")).isNull();
    }

    @Test
    void runRecordsSkippedAndAlreadyKnownCounts() {
        listingSnapshotService.recordFetch("sreality", "4701", fixture("detail_success.json"));
        stubSearch(okJson(fixture("search_page_skipped_known.json")));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4702")).willReturn(aResponse().withStatus(404)));

        job.run();

        Map<String, Object> run = latestRun();
        assertThat(run.get("status")).isEqualTo(ScrapeRunStatus.SUCCEEDED.name());
        assertThat(run.get("found_count")).isEqualTo(2);
        assertThat(run.get("already_known_count")).isEqualTo(1);
        assertThat(run.get("skipped_count")).isEqualTo(1);
        assertThat(run.get("new_count")).isEqualTo(0);
        sreality.verify(0, getRequestedFor(urlEqualTo("/api/v1/estates/4701")));
    }

    @Test
    void failedRunIsClosedAsFailedWithTheCause() {
        stubSearch(aResponse().withStatus(500));

        job.run();

        Map<String, Object> run = latestRun();
        assertThat(run.get("status")).isEqualTo(ScrapeRunStatus.FAILED.name());
        assertThat(run.get("finished_at")).isNotNull();
        assertThat((String) run.get("error_details")).contains("SrealityApiException").contains("HTTP 500");
    }

    @Test
    void listingMissingFromARunIsMarkedRemovedAndClearedWhenItReappears() {
        String detail = fixture("detail_success.json");
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4301")).willReturn(okJson(detail)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4302")).willReturn(okJson(detail)));

        stubSearch(okJson(fixture("search_page_presence_both.json")));
        job.run();
        assertThat(removedAt(4301)).isNull();
        assertThat(removedAt(4302)).isNull();

        stubSearch(okJson(fixture("search_page_presence_one.json")));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4302")).willReturn(aResponse().withStatus(404)));
        job.run();
        assertThat(removedAt(4301)).isNull();
        assertThat(removedAt(4302)).isNotNull();
        assertThat(latestRun().get("removed_count")).isEqualTo(1);

        stubSearch(okJson(fixture("search_page_presence_both.json")));
        job.run();
        assertThat(removedAt(4302)).isNull();
        assertThat(latestRun().get("removed_count")).isEqualTo(0);
        sreality.verify(2, getRequestedFor(urlEqualTo("/api/v1/estates/4302")));
    }

    @Test
    void unseenListingStillOnlineAboveThePriceCapStaysActiveAndItsNewPriceIsRecorded() {
        String detail = fixture("detail_success.json");
        String aboveCap = detail.replace("\"price_summary_czk\": 3590000.0", "\"price_summary_czk\": 5900000.0");
        assertThat(aboveCap).isNotEqualTo(detail);
        listingSnapshotService.recordFetch("sreality", "4801", detail);
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4301")).willReturn(okJson(detail)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4801")).willReturn(okJson(aboveCap)));
        stubSearch(okJson(fixture("search_page_presence_one.json")));

        job.run();

        assertThat(removedAt(4801)).isNull();
        assertThat(snapshotCount(4801)).isEqualTo(2);
        assertThat(pricePoints(4801)).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("3590000"), new BigDecimal("5900000"));
        assertThat(latestRun().get("updated_count")).isEqualTo(1);
        sreality.verify(1, getRequestedFor(urlEqualTo("/api/v1/estates/4801")));
    }

    @Test
    void unseenListingIsMarkedRemovedOnlyWhenItsDetailIsGone() {
        for (String id : List.of("4811", "4812", "4813", "4814")) {
            listingSnapshotService.recordFetch("sreality", id, "{}");
        }
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4301")).willReturn(okJson(fixture("detail_success.json"))));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4811")).willReturn(aResponse().withStatus(404)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4812")).willReturn(aResponse().withStatus(410)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4813")).willReturn(aResponse().withStatus(403)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4814")).willReturn(aResponse().withStatus(500)));
        stubSearch(okJson(fixture("search_page_presence_one.json")));

        job.run();

        assertThat(removedAt(4811)).isNotNull();
        assertThat(removedAt(4812)).isNotNull();
        assertThat(removedAt(4813)).isNull();
        assertThat(removedAt(4814)).isNull();
        Map<String, Object> run = latestRun();
        assertThat(run.get("status")).isEqualTo(ScrapeRunStatus.SUCCEEDED.name());
        assertThat(run.get("error_count")).isEqualTo(2);
    }

    @Test
    void failedRunMarksNothingRemoved() {
        listingSnapshotService.recordFetch("sreality", "4401", "{}");
        stubSearch(aResponse().withStatus(500));

        job.run();

        assertThat(removedAt(4401)).isNull();
    }

    @Test
    void runThatFindsNothingMarksNothingRemoved() {
        listingSnapshotService.recordFetch("sreality", "4402", "{}");
        stubSearch(okJson(fixture("search_count_empty.json")));

        job.run();

        assertThat(latestRun().get("status")).isEqualTo(ScrapeRunStatus.SUCCEEDED.name());
        assertThat(removedAt(4402)).isNull();
        assertThat(latestRun().get("removed_count")).isEqualTo(0);
    }

    @Test
    void knownListingsLastFetchedBeforeTheRefetchIntervalAreFetchedAgain() {
        String detail = fixture("detail_success.json");
        String repriced = detail.replace("\"price_czk\": 3590000.0", "\"price_czk\": 3490000.0");
        assertThat(repriced).isNotEqualTo(detail);
        for (String id : List.of("4501", "4502", "4503")) {
            listingSnapshotService.recordFetch("sreality", id, detail);
        }
        jdbc.update("UPDATE listing SET last_fetched_at = now() - interval '8 days'"
                + " WHERE source = 'sreality' AND external_id IN ('4501', '4502')");
        Instant beforeRun = Instant.now();
        stubSearch(okJson(fixture("search_page_refetch.json")));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4501")).willReturn(okJson(repriced)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4502")).willReturn(okJson(detail)));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4503")).willReturn(okJson(detail)));

        job.run();

        assertThat(snapshotCount(4501)).isEqualTo(2);
        assertThat(snapshotCount(4502)).isEqualTo(1);
        assertThat(lastFetchedAt(4502)).isAfter(beforeRun);
        sreality.verify(0, getRequestedFor(urlEqualTo("/api/v1/estates/4503")));
        Map<String, Object> run = latestRun();
        assertThat(run.get("new_count")).isEqualTo(0);
        assertThat(run.get("updated_count")).isEqualTo(1);
    }

    @Test
    void rentalsAreMarkedRemovedAndRefetchedLikeSales() {
        String detail = fixture("detail_success.json");
        for (String id : List.of("4301", "4601", "4602")) {
            sreality.stubFor(get(urlEqualTo("/api/v1/estates/" + id)).willReturn(okJson(detail)));
        }
        stubSearch(okJson(fixture("search_page_presence_one.json")));
        stubRentalSearch(okJson(fixture("search_page_rentals.json")));
        job.run();
        assertThat(snapshotCount(4601)).isEqualTo(1);
        assertThat(snapshotCount(4602)).isEqualTo(1);

        jdbc.update("UPDATE listing SET last_fetched_at = now() - interval '8 days'"
                + " WHERE source = 'sreality' AND external_id = '4601'");
        stubRentalSearch(okJson(fixture("search_page_rentals_one.json")));
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/4602")).willReturn(aResponse().withStatus(404)));
        job.run();

        assertThat(removedAt(4301)).isNull();
        assertThat(removedAt(4601)).isNull();
        assertThat(removedAt(4602)).isNotNull();
        assertThat(latestRun().get("removed_count")).isEqualTo(1);
        sreality.verify(2, getRequestedFor(urlEqualTo("/api/v1/estates/4601")));
        sreality.verify(2, getRequestedFor(urlEqualTo("/api/v1/estates/4602")));
        sreality.verify(0, getRequestedFor(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("category_type_cb", equalTo("2"))
                .withQueryParam("price_to", matching(".*")));
    }

    /** Region 1 of the sale search answers with {@code region1}; everything else is empty unless stubbed. */
    private static void stubSearch(ResponseDefinitionBuilder region1) {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH)).atPriority(10)
                .willReturn(okJson(fixture("search_count_empty.json"))));
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("category_type_cb", equalTo("1"))
                .withQueryParam("locality_region_id", equalTo("1"))
                .willReturn(region1));
    }

    /** Region 1 of the rental search answers with {@code region1}. */
    private static void stubRentalSearch(ResponseDefinitionBuilder region1) {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("category_type_cb", equalTo("2"))
                .withQueryParam("locality_region_id", equalTo("1"))
                .willReturn(region1));
    }

    private int snapshotCount(long estateId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM listing_snapshot s JOIN listing l ON l.id = s.listing_id
                WHERE l.source = 'sreality' AND l.external_id = ?
                """, Integer.class, String.valueOf(estateId));
    }

    private List<BigDecimal> pricePoints(long estateId) {
        return jdbc.queryForList("""
                SELECT p.price_czk FROM listing_price_point p JOIN listing l ON l.id = p.listing_id
                WHERE l.source = 'sreality' AND l.external_id = ? ORDER BY p.observed_at
                """, BigDecimal.class, String.valueOf(estateId));
    }

    private Instant lastFetchedAt(long estateId) {
        return jdbc.queryForObject("SELECT last_fetched_at FROM listing WHERE source = 'sreality' AND external_id = ?",
                Timestamp.class, String.valueOf(estateId)).toInstant();
    }

    private Object removedAt(long estateId) {
        return jdbc.queryForObject("SELECT removed_at FROM listing WHERE source = 'sreality' AND external_id = ?",
                Object.class, String.valueOf(estateId));
    }

    private Map<String, Object> latestRun() {
        return jdbc.queryForMap("SELECT * FROM scrape_run ORDER BY id DESC LIMIT 1");
    }

    private static String fixture(String name) {
        String path = "fixtures/sreality/" + name;
        try (InputStream in = SrealityScrapeJobTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
