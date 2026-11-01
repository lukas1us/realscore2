package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Records snapshots through {@link ListingSnapshotService}, which parses them automatically (TASK-33), then reads
 * scores over HTTP. The Postgres container is shared with other tests, so each test uses its own region and passes
 * it as the region filter.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "realscore.analysis.min-sample-size=2",
        "realscore.analysis.large-cities=Testov",
        "realscore.analysis.scores-limit=2"})
@Import(PostgresContainerConfig.class)
class ListingScoreControllerTest {

    private static final String SOURCE = "score-http";
    private static final int SALE = 1;
    private static final int RENT = 2;
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    @LocalServerPort
    int port;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private JdbcTemplate jdbc;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void returnsTheBestScoresFirstUpToTheDefaultLimit() {
        String region = "Kraj scores-default";
        String best = record(region, "Testov", "2+kk", 4, SALE, 5_000_000);
        String second = record(region, "Testov", "2+kk", 4, SALE, 6_000_000);
        record(region, "Testov", "2+kk", 4, SALE, 7_000_000);

        assertThat(scores("/api/analysis/scores?region={region}", region))
                .extracting(ListingScore::externalId).containsExactly(best, second);
    }

    @Test
    void theLimitParameterOverridesTheDefault() {
        String region = "Kraj scores-limit";
        record(region, "Testov", "2+kk", 4, SALE, 5_000_000);
        record(region, "Testov", "2+kk", 4, SALE, 6_000_000);
        record(region, "Testov", "2+kk", 4, SALE, 7_000_000);

        assertThat(scores("/api/analysis/scores?region={region}&limit=1", region)).hasSize(1);
        assertThat(scores("/api/analysis/scores?region={region}&limit=10", region)).hasSize(3);
    }

    @Test
    void filtersByCityAndDispositionIgnoringCase() {
        String region = "Kraj scores-filters";
        String testov2kk = record(region, "Testov", "2+kk", 4, SALE, 5_000_000);
        record(region, "Testov", "2+kk", 4, SALE, 6_000_000);
        String testov3kk = record(region, "Testov", "3+kk", 6, SALE, 7_000_000);
        record(region, "Testov", "3+kk", 6, SALE, 8_000_000);
        String other = record(region, "Jinov", "3+kk", 6, SALE, 7_000_000);

        assertThat(scores("/api/analysis/scores?region={region}&disposition={d}&limit=10", region, "3+KK"))
                .extracting(ListingScore::externalId).contains(testov3kk, other).doesNotContain(testov2kk);
        assertThat(scores("/api/analysis/scores?region={region}&city={city}&limit=10", region, "jinov"))
                .extracting(ListingScore::externalId).containsExactly(other);
    }

    @Test
    void anInvalidLimitIsABadRequest() {
        assertThatThrownBy(() -> scores("/api/analysis/scores?limit=0"))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void theListingEndpointIncludesTheScoreOfAScorableSaleFlatOnly() {
        String region = "Kraj scores-listing";
        String sale = record(region, "Testov", "2+kk", 4, SALE, 5_000_000);
        record(region, "Testov", "2+kk", 4, SALE, 6_000_000);
        String removed = record(region, "Testov", "2+kk", 4, SALE, 7_000_000);
        String rental = record(region, "Testov", "2+kk", 4, RENT, 20_000);
        record(region, "Testov", "2+kk", 4, RENT, 30_000);
        jdbc.update("UPDATE listing SET removed_at = now() WHERE source = ? AND external_id = ?", SOURCE, removed);

        ListingScore score = listing(sale).score();
        assertThat(score).isNotNull();
        assertThat(score.discount()).isNotNull();
        assertThat(score.rentalYield()).isNotNull();
        assertThat(listing(removed).score()).isNull();
        assertThat(listing(rental).score()).isNull();
    }

    private List<ListingScore> scores(String uri, Object... variables) {
        return client.get().uri(uri, variables).retrieve().body(new ParameterizedTypeReference<>() {
        });
    }

    private ListingResponse listing(String externalId) {
        return client.get().uri("/api/listings/{source}/{externalId}", SOURCE, externalId).retrieve()
                .body(ListingResponse.class);
    }

    /** Records a 50 m² flat in the given city (not a large city, so grouped by city) and returns its ID. */
    private String record(String region, String city, String disposition, int dispositionCode, int transactionType,
            long price) {
        String externalId = String.valueOf(NEXT_ID.incrementAndGet());
        String payload = """
                {"result": {
                  "price_summary_czk": %d,
                  "usable_area": 50,
                  "category_sub_cb": {"name": "%s", "value": %d},
                  "category_type_cb": {"name": "x", "value": %d},
                  "category_main_cb": {"name": "x", "value": 1},
                  "locality": {"region": "%s", "district": "Testov-okres", "city": "%s"}
                }}
                """.formatted(price, disposition, dispositionCode, transactionType, region, city);
        listingSnapshotService.recordFetch(SOURCE, externalId, payload);
        return externalId;
    }
}
