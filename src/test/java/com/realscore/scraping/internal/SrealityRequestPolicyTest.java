package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.ResourceAccessException;

/**
 * Exercises {@link SrealityRequestPolicy} through {@link SrealityClient}, with 3 attempts, a 2s initial backoff and a
 * 500ms request delay. Time is fake: every wait is recorded instead of slept.
 */
class SrealityRequestPolicyTest {

    private static final long ESTATE_ID = 2494705740L;
    private static final String DETAIL_PATH = "/api/v1/estates/" + ESTATE_ID;
    private static final String BODY = "{\"result\": {}}";

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private FakeTime time;
    private SrealityClient client;

    @BeforeEach
    void setUp() {
        time = new FakeTime();
        client = SrealityTestSupport.client(SrealityTestSupport.properties(sreality.baseUrl(), 950, 100), time);
    }

    @Test
    void tooManyRequestsIsRetriedAfterBackoff() {
        respondInTurn(aResponse().withStatus(429), okJson(BODY));

        assertThat(client.fetchEstateDetail(ESTATE_ID)).contains(BODY);

        sreality.verify(2, getRequestedFor(urlEqualTo(DETAIL_PATH)));
        assertThat(time.getSleeps()).containsExactly(Duration.ofSeconds(2));
    }

    @Test
    void tooManyRequestsOnEveryAttemptGivesUpWithDoublingBackoff() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID))
                .isInstanceOfSatisfying(SrealityApiException.class, e -> assertThat(e.statusCode()).isEqualTo(429));

        sreality.verify(3, getRequestedFor(urlEqualTo(DETAIL_PATH)));
        assertThat(time.getSleeps()).containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404})
    void forbiddenAndNotFoundAreSkippedWithoutRetry(int status) {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(status)));

        assertThat(client.fetchEstateDetail(ESTATE_ID)).isEmpty();

        sreality.verify(1, getRequestedFor(urlEqualTo(DETAIL_PATH)));
        assertThat(time.getSleeps()).isEmpty();
    }

    @Test
    void connectionErrorIsRetried() {
        respondInTurn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER), okJson(BODY));

        assertThat(client.fetchEstateDetail(ESTATE_ID)).contains(BODY);

        sreality.verify(2, getRequestedFor(urlEqualTo(DETAIL_PATH)));
        assertThat(time.getSleeps()).containsExactly(Duration.ofSeconds(2));
    }

    @Test
    void connectionErrorOnEveryAttemptIsRethrown() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID)).isInstanceOf(ResourceAccessException.class);

        sreality.verify(3, getRequestedFor(urlEqualTo(DETAIL_PATH)));
    }

    @Test
    void serverErrorIsRetried() {
        respondInTurn(aResponse().withStatus(503), okJson(BODY));

        assertThat(client.fetchEstateDetail(ESTATE_ID)).contains(BODY);

        sreality.verify(2, getRequestedFor(urlEqualTo(DETAIL_PATH)));
    }

    @Test
    void searchRequestsAreRetriedToo() {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH)).inScenario("search")
                .whenScenarioStateIs(STARTED).willReturn(aResponse().withStatus(429)).willSetStateTo("ok"));
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH)).inScenario("search")
                .whenScenarioStateIs("ok").willReturn(okJson("{\"pagination\": {\"total\": 7}}")));

        assertThat(client.countEstates(Map.of("locality_region_id", "10"))).isEqualTo(7);
    }

    @Test
    void requestsAreSpacedByAtLeastTheRequestDelay() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(okJson(BODY)));

        client.fetchEstateDetail(ESTATE_ID);
        time.advance(Duration.ofMillis(200));
        client.fetchEstateDetail(ESTATE_ID);
        assertThat(time.getSleeps()).containsExactly(Duration.ofMillis(300));

        time.advance(Duration.ofSeconds(1));
        client.fetchEstateDetail(ESTATE_ID);
        assertThat(time.getSleeps()).containsExactly(Duration.ofMillis(300));
    }

    /** The detail endpoint answers with {@code first}, then with {@code second} from then on. */
    private static void respondInTurn(ResponseDefinitionBuilder first, ResponseDefinitionBuilder second) {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).inScenario("detail")
                .whenScenarioStateIs(STARTED).willReturn(first).willSetStateTo("second"));
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).inScenario("detail")
                .whenScenarioStateIs("second").willReturn(second));
    }
}
