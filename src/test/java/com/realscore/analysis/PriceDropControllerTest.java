package com.realscore.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Records snapshots through {@link ListingSnapshotService}, which parses them and their price history automatically
 * (TASK-33, TASK-23.1). The Postgres container is shared with other tests whose listings may also have dropped, so
 * each test only looks at drops from its own source.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfig.class)
class PriceDropControllerTest {

    private static final int SALE = 1;
    private static final int RENT = 2;

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
    void listsActiveDropsLargestFirst() {
        String source = "drops-order";
        record(source, "small", SALE, "3590000", "3390000");
        record(source, "large", SALE, "4000000", "3600000");
        record(source, "rise", SALE, "3000000", "3100000");

        List<PriceDrop> drops = drops("/api/analysis/price-drops", source);

        assertThat(drops).extracting(PriceDrop::externalId).containsExactly("large", "small");
        assertThat(drops.getFirst().dropPercent()).isEqualByComparingTo("10.0");
        assertThat(drops.getFirst().transactionType()).isEqualTo(TransactionType.SALE);
    }

    @Test
    void includesTheListingUrl() {
        String source = "drops-url";
        for (String price : new String[] {"3590000", "3390000"}) {
            listingSnapshotService.recordFetch(source, "77", """
                    {"result": {"price_summary_czk": %s, "category_type_cb": {"value": 2},
                     "category_main_cb": {"value": 1}, "category_sub_cb": {"name": "1+kk"},
                     "locality": {"city_seo_name": "plzen", "citypart_seo_name": "jizni-predmesti"}, "hash_id": 77}}
                    """.formatted(price));
        }

        assertThat(drops("/api/analysis/price-drops", source)).extracting(PriceDrop::url)
                .containsExactly("https://www.sreality.cz/detail/pronajem/byt/1+kk/plzen-jizni-predmesti/77");
    }

    @Test
    void filtersBySaleOrRent() {
        String source = "drops-type";
        record(source, "sale", SALE, "3590000", "3390000");
        record(source, "rent", RENT, "20000", "18000");

        assertThat(drops("/api/analysis/price-drops?type=sale", source))
                .extracting(PriceDrop::externalId).containsExactly("sale");
        assertThat(drops("/api/analysis/price-drops?type=rent", source))
                .extracting(PriceDrop::externalId).containsExactly("rent");
        assertThat(drops("/api/analysis/price-drops?days=7", source))
                .extracting(PriceDrop::externalId).containsExactlyInAnyOrder("sale", "rent");
    }

    @Test
    void leavesOutRemovedListings() {
        String source = "drops-removed";
        record(source, "removed", SALE, "3590000", "3390000");
        listingSnapshotService.markRemoved(source, List.of("removed"), Instant.now());

        assertThat(drops("/api/analysis/price-drops", source)).isEmpty();
    }

    @Test
    void rejectsInvalidParameters() {
        assertThatThrownBy(() -> drops("/api/analysis/price-drops?days=0", "any"))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
        assertThatThrownBy(() -> drops("/api/analysis/price-drops?type=lease", "any"))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    /** Records one listing with the given prices, one snapshot per price, oldest first. */
    private void record(String source, String externalId, int transactionType, String... prices) {
        for (String price : prices) {
            listingSnapshotService.recordFetch(source, externalId, """
                    {"result": {"price_summary_czk": %s, "category_type_cb": {"value": %d}}}
                    """.formatted(price, transactionType));
        }
    }

    private List<PriceDrop> drops(String uri, String source) {
        List<PriceDrop> all = client.get().uri(uri).retrieve().body(new ParameterizedTypeReference<>() {
        });
        return all.stream().filter(drop -> drop.source().equals(source)).toList();
    }
}
