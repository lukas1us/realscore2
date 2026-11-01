package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Records snapshots through {@link ListingSnapshotService}, which parses them automatically (TASK-33), then reads
 * them through the endpoint. Each test uses its own source.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfig.class)
class ListingControllerTest {

    /** Valid JSON whose construction type is longer than listing_detail.construction_type allows. */
    private static final String UNPARSEABLE_PAYLOAD = """
            {"result": {"price_summary_czk": 3590000, "building_type": {"name": "%s", "value": 1}}}
            """.formatted("x".repeat(60));

    @LocalServerPort
    int port;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void returnsDetailsPriceHistoryAndStats() throws Exception {
        String source = "listing-endpoint";
        String payload = Files.readString(Path.of("src/test/resources/fixtures/sreality/detail_success.json"));
        listingSnapshotService.recordFetch(source, "1", payload);
        listingSnapshotService.recordFetch(source, "1", payload.replace("\"price_summary_czk\": 3590000.0",
                "\"price_summary_czk\": 3390000.0"));

        ListingResponse listing = get(source, "1");

        assertThat(listing.firstSeenAt()).isNotNull();
        assertThat(listing.lastSeenAt()).isNotNull();
        assertThat(listing.removedAt()).isNull();
        assertThat(listing.details().ownership()).isEqualTo("OV");
        assertThat(listing.details().priceCzk()).isEqualByComparingTo("3390000");
        assertThat(listing.priceHistory()).extracting(point -> point.priceCzk().toBigInteger().toString())
                .containsExactly("3590000", "3390000");
        assertThat(listing.stats().daysOnMarket()).isZero();
        assertThat(listing.stats().priceDrops()).isEqualTo(1);
        assertThat(listing.stats().priceChangePercent()).isEqualByComparingTo("-5.6");
    }

    @Test
    void aListingThatWasNeverParsedHasNoDetailsAndNoHistory() {
        String source = "listing-endpoint-unparsed";
        listingSnapshotService.recordFetch(source, "1", UNPARSEABLE_PAYLOAD);

        ListingResponse listing = get(source, "1");

        assertThat(listing.details()).isNull();
        assertThat(listing.priceHistory()).isEmpty();
        assertThat(listing.stats().priceChangePercent()).isNull();
    }

    @Test
    void anUnknownListingIsNotFound() {
        assertThatThrownBy(() -> get("listing-endpoint-unknown", "1"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    private ListingResponse get(String source, String externalId) {
        return client.get()
                .uri("/api/listings/{source}/{externalId}", source, externalId)
                .retrieve()
                .body(ListingResponse.class);
    }
}
