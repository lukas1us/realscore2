package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.realscore.PostgresContainerConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class SrealityEstateFetcherTest {

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static String fixture;

    @DynamicPropertySource
    static void srealityBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("realscore.sreality.base-url", sreality::baseUrl);
        registry.add("realscore.sreality.request-delay", () -> "0ms");
        registry.add("realscore.sreality.initial-backoff", () -> "1ms");
    }

    @Autowired
    private SrealityEstateFetcher fetcher;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void loadFixture() throws IOException {
        try (InputStream in = SrealityEstateFetcherTest.class.getClassLoader()
                .getResourceAsStream("fixtures/sreality/detail_success.json")) {
            assertThat(in).isNotNull();
            fixture = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void newEstateCreatesListingAndFirstSnapshot() {
        long estateId = 1001;
        stubDetail(estateId, fixture);

        assertThat(fetcher.fetch(estateId)).isTrue();

        assertThat(listingCount(estateId)).isEqualTo(1);
        assertThat(snapshotCount(estateId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT s.payload = ?::jsonb
                FROM listing_snapshot s JOIN listing l ON l.id = s.listing_id
                WHERE l.source = 'sreality' AND l.external_id = ?
                """, Boolean.class, fixture, String.valueOf(estateId))).isTrue();
    }

    @Test
    void unchangedPayloadUpdatesLastSeenAtWithoutNewSnapshot() {
        long estateId = 1002;
        stubDetail(estateId, fixture);
        fetcher.fetch(estateId);
        Timestamp firstSeen = lastSeenAt(estateId);

        fetcher.fetch(estateId);

        assertThat(snapshotCount(estateId)).isEqualTo(1);
        assertThat(lastSeenAt(estateId)).isAfter(firstSeen);
    }

    @Test
    void changedPayloadUpdatesListingAndWritesNewSnapshot() {
        long estateId = 1003;
        stubDetail(estateId, fixture);
        fetcher.fetch(estateId);
        Timestamp firstSeen = lastSeenAt(estateId);

        String repriced = fixture.replace("\"price_czk\": 3590000.0", "\"price_czk\": 3490000.0");
        assertThat(repriced).isNotEqualTo(fixture);
        stubDetail(estateId, repriced);
        fetcher.fetch(estateId);

        assertThat(snapshotCount(estateId)).isEqualTo(2);
        assertThat(lastSeenAt(estateId)).isAfter(firstSeen);
    }

    @Test
    void goneEstateIsSkippedWithoutRecordingAnything() {
        long estateId = 1004;
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/" + estateId)).willReturn(aResponse().withStatus(404)));

        assertThat(fetcher.fetch(estateId)).isFalse();

        assertThat(listingCount(estateId)).isZero();
    }

    @Test
    void emptyBodyRecordsNothing() {
        long estateId = 1005;
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/" + estateId)).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> fetcher.fetch(estateId)).isInstanceOf(IllegalStateException.class);

        assertThat(listingCount(estateId)).isZero();
    }

    private static void stubDetail(long estateId, String body) {
        sreality.stubFor(get(urlEqualTo("/api/v1/estates/" + estateId)).willReturn(okJson(body)));
    }

    private int listingCount(long estateId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM listing WHERE source = 'sreality' AND external_id = ?",
                Integer.class, String.valueOf(estateId));
    }

    private int snapshotCount(long estateId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM listing_snapshot s JOIN listing l ON l.id = s.listing_id
                WHERE l.source = 'sreality' AND l.external_id = ?
                """, Integer.class, String.valueOf(estateId));
    }

    private Timestamp lastSeenAt(long estateId) {
        return jdbc.queryForObject(
                "SELECT last_seen_at FROM listing WHERE source = 'sreality' AND external_id = ?",
                Timestamp.class, String.valueOf(estateId));
    }
}
