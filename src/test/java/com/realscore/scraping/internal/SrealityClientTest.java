package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.math.BigDecimal;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SrealityClientTest {

    private static final long ESTATE_ID = 2494705740L;
    private static final String DETAIL_PATH = "/api/v1/estates/" + ESTATE_ID;
    private static final String SEARCH_PATH = SrealityTestSupport.SEARCH_PATH;
    private static final Map<String, String> PRAHA_FLATS = Map.of("category_main_cb", "1", "locality_region_id", "10");

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private SrealityClient client;

    @BeforeEach
    void setUp() {
        client = SrealityTestSupport.client(SrealityTestSupport.properties(sreality.baseUrl(), 950, 100), new FakeTime());
    }

    @Test
    void returnsRawDetailJson() throws IOException {
        String fixture = readFixture("fixtures/sreality/detail_success.json");
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(okJson(fixture)));

        Optional<String> body = client.fetchEstateDetail(ESTATE_ID);

        assertThat(body).contains(SrealityPayloadScrubber.scrub(fixture));
        assertThat(body.orElseThrow()).doesNotContain("user_email", "user_phones", "web_visible_url");
        sreality.verify(getRequestedFor(urlEqualTo(DETAIL_PATH))
                .withHeader("User-Agent", equalTo("test-agent"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Referer", equalTo("https://www.sreality.cz/")));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 410})
    void forbiddenOrGoneDetailIsSkipped(int status) {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(status)));

        assertThat(client.fetchEstateDetail(ESTATE_ID)).isEmpty();
    }

    @Test
    void fetchDetailReturnsTheBodyOf200AsFound() throws IOException {
        String fixture = readFixture("fixtures/sreality/detail_success.json");
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(okJson(fixture)));

        assertThat(client.fetchDetail(ESTATE_ID)).isEqualTo(new EstateDetail.Found(SrealityPayloadScrubber.scrub(fixture)));
    }

    @Test
    void detailBodyThatIsNotJsonFailsAsIllegalState() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(200).withBody("<html>")));

        assertThatThrownBy(() -> client.fetchDetail(ESTATE_ID)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 410})
    void fetchDetailTells404And410AsGone(int status) {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(status)));

        assertThat(client.fetchDetail(ESTATE_ID)).isEqualTo(new EstateDetail.Gone(status));
    }

    @Test
    void fetchDetailTells403AsForbiddenNotGone() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(403)));

        assertThat(client.fetchDetail(ESTATE_ID)).isEqualTo(new EstateDetail.Forbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   \n"})
    void emptyOrBlankDetailBodyFailsInsteadOfReturningNothing(String body) {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(200).withBody(body)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty body")
                .hasMessageContaining(String.valueOf(ESTATE_ID));
    }

    @Test
    void otherClientErrorRaisesSrealityApiException() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID))
                .isInstanceOfSatisfying(SrealityApiException.class, e -> {
                    assertThat(e.statusCode()).isEqualTo(400);
                    assertThat(e.estateId()).isEqualTo(ESTATE_ID);
                    assertThat(e.requestPath()).isEqualTo(DETAIL_PATH);
                });
    }

    @Test
    void persistentServerErrorRaisesSrealityApiExceptionAfterRetries() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID))
                .isInstanceOfSatisfying(SrealityApiException.class,
                        e -> assertThat(e.statusCode()).isEqualTo(500));
        sreality.verify(SrealityTestSupport.MAX_ATTEMPTS, getRequestedFor(urlEqualTo(DETAIL_PATH)));
    }

    @Test
    void countEstatesReturnsPaginationTotal() throws IOException {
        String fixture = readFixture("fixtures/sreality/search_count_only.json");
        sreality.stubFor(get(urlPathEqualTo(SEARCH_PATH)).willReturn(okJson(fixture)));

        long total = client.countEstates(PRAHA_FLATS);

        assertThat(total).isEqualTo(9561);
        sreality.verify(getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("category_main_cb", equalTo("1"))
                .withQueryParam("locality_region_id", equalTo("10"))
                .withQueryParam("limit", equalTo("0"))
                .withQueryParam("offset", equalTo("0"))
                .withHeader("User-Agent", equalTo("test-agent")));
    }

    @Test
    void failedCountRaisesSrealityApiExceptionWithoutEstateId() {
        sreality.stubFor(get(urlPathEqualTo(SEARCH_PATH)).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> client.countEstates(PRAHA_FLATS))
                .isInstanceOfSatisfying(SrealityApiException.class, e -> {
                    assertThat(e.statusCode()).isEqualTo(429);
                    assertThat(e.requestPath()).isEqualTo(SEARCH_PATH);
                    assertThat(e.estateId()).isNull();
                });
    }

    @Test
    void countWithEmptyBodyFailsInsteadOfReturningNothing() {
        sreality.stubFor(get(urlPathEqualTo(SEARCH_PATH)).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> client.countEstates(PRAHA_FLATS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pagination.total");
    }

    @Test
    void countWithoutTotalFailsInsteadOfReturningZero() {
        sreality.stubFor(get(urlPathEqualTo(SEARCH_PATH)).willReturn(okJson("{\"pagination\": {\"limit\": 0}}")));

        assertThatThrownBy(() -> client.countEstates(PRAHA_FLATS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pagination.total");
    }

    @Test
    void searchEstatesReturnsPageOfIdsAndTotal() throws IOException {
        String fixture = readFixture("fixtures/sreality/search_page_small_region_1.json");
        sreality.stubFor(get(urlPathEqualTo(SEARCH_PATH)).willReturn(okJson(fixture)));

        SearchPage page = client.searchEstates(PRAHA_FLATS, 2, 0);

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.results()).containsExactly(
                new SearchPage.Result(101L, new BigDecimal("3590000.0")),
                new SearchPage.Result(102L, new BigDecimal("0.0")));
        sreality.verify(getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("category_main_cb", equalTo("1"))
                .withQueryParam("locality_region_id", equalTo("10"))
                .withQueryParam("limit", equalTo("2"))
                .withQueryParam("offset", equalTo("0")));
    }

    @Test
    void failedSearchRaisesSrealityApiException() {
        sreality.stubFor(get(urlPathEqualTo(SEARCH_PATH)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.searchEstates(PRAHA_FLATS, 2, 0))
                .isInstanceOfSatisfying(SrealityApiException.class,
                        e -> assertThat(e.statusCode()).isEqualTo(503));
    }

    private static String readFixture(String path) throws IOException {
        try (InputStream in = SrealityClientTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
