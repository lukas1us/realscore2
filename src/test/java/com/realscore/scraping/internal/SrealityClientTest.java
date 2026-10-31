package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SrealityClientTest {

    private static final long ESTATE_ID = 2494705740L;
    private static final String DETAIL_PATH = "/api/v1/estates/" + ESTATE_ID;

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private SrealityClient client;

    @BeforeEach
    void setUp() {
        client = new SrealityClient(new SrealityProperties(sreality.baseUrl(), "test-agent"));
    }

    @Test
    void returnsRawDetailJson() throws IOException {
        String fixture = readFixture("fixtures/sreality/detail_success.json");
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(okJson(fixture)));

        String body = client.fetchEstateDetail(ESTATE_ID);

        assertThat(body).isEqualTo(fixture);
        sreality.verify(getRequestedFor(urlEqualTo(DETAIL_PATH))
                .withHeader("User-Agent", equalTo("test-agent"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Referer", equalTo("https://www.sreality.cz/")));
    }

    @Test
    void notFoundRaisesSrealityApiException() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID))
                .isInstanceOfSatisfying(SrealityApiException.class, e -> {
                    assertThat(e.statusCode()).isEqualTo(404);
                    assertThat(e.estateId()).isEqualTo(ESTATE_ID);
                });
    }

    @Test
    void serverErrorRaisesSrealityApiException() {
        sreality.stubFor(get(urlEqualTo(DETAIL_PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.fetchEstateDetail(ESTATE_ID))
                .isInstanceOfSatisfying(SrealityApiException.class,
                        e -> assertThat(e.statusCode()).isEqualTo(500));
    }

    private static String readFixture(String path) throws IOException {
        try (InputStream in = SrealityClientTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
