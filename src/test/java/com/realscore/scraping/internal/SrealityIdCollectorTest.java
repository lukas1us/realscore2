package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Region 1 is small (3 estates, collected directly); region 10 is over the threshold and split by disposition,
 * with estate 203 in both its 2+kk and 3+kk buckets. Every other region and disposition is empty.
 * A small bucket's count request is answered with its first page, whose {@code pagination.total} is the count.
 */
class SrealityIdCollectorTest {

    private static final String SEARCH_PATH = SrealityTestSupport.SEARCH_PATH;
    private static final long PRICE_CAP = 5_000_000;
    private static final int BUCKET_THRESHOLD = 5;
    private static final int PAGE_SIZE = 2;
    private static final String SMALL_REGION = "1";
    private static final String LARGE_REGION = "10";
    private static final String DISPOSITION_2KK = "4";
    private static final String DISPOSITION_3KK = "6";

    @RegisterExtension
    static WireMockExtension sreality = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private SrealityIdCollector collector;

    @BeforeEach
    void setUp() {
        SrealityProperties properties =
                SrealityTestSupport.properties(sreality.baseUrl(), BUCKET_THRESHOLD, PAGE_SIZE);
        collector = new SrealityIdCollector(SrealityTestSupport.client(properties, new FakeTime()), properties);

        sreality.stubFor(search().atPriority(10).willReturn(fixture("search_count_empty.json")));

        sreality.stubFor(region(SMALL_REGION).withQueryParam("limit", equalTo("0"))
                .willReturn(fixture("search_page_small_region_1.json")));
        sreality.stubFor(page(region(SMALL_REGION), 0).willReturn(fixture("search_page_small_region_1.json")));
        sreality.stubFor(page(region(SMALL_REGION), 2).willReturn(fixture("search_page_small_region_2.json")));

        sreality.stubFor(region(LARGE_REGION).withQueryParam("category_sub_cb", absent())
                .withQueryParam("limit", equalTo("0"))
                .willReturn(fixture("search_count_only.json")));
        sreality.stubFor(disposition(DISPOSITION_2KK).withQueryParam("limit", equalTo("0"))
                .willReturn(fixture("search_page_2kk_1.json")));
        sreality.stubFor(page(disposition(DISPOSITION_2KK), 0).willReturn(fixture("search_page_2kk_1.json")));
        sreality.stubFor(page(disposition(DISPOSITION_2KK), 2).willReturn(fixture("search_page_2kk_2.json")));
        sreality.stubFor(disposition(DISPOSITION_3KK).withQueryParam("limit", equalTo("0"))
                .willReturn(fixture("search_page_3kk.json")));
        sreality.stubFor(page(disposition(DISPOSITION_3KK), 0).willReturn(fixture("search_page_3kk.json")));
    }

    @Test
    void collectsSmallRegionDirectlyAndLargeRegionPerDispositionWithoutDuplicates() {
        assertThat(collector.collectSaleApartmentIds(PRICE_CAP))
                .containsExactlyInAnyOrder(101L, 102L, 103L, 201L, 202L, 203L, 204L);
    }

    @Test
    void probesEveryRegionAndOnlySplitsTheLargeOne() {
        collector.collectSaleApartmentIds(PRICE_CAP);

        for (int regionId : SrealityIdCollector.REGION_IDS) {
            sreality.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("category_main_cb", equalTo("1"))
                    .withQueryParam("category_type_cb", equalTo("1"))
                    .withQueryParam("price_to", equalTo(String.valueOf(PRICE_CAP)))
                    .withQueryParam("locality_region_id", equalTo(String.valueOf(regionId)))
                    .withQueryParam("category_sub_cb", absent())
                    .withQueryParam("limit", equalTo("0")));
        }
        for (int disposition : SrealityIdCollector.DISPOSITION_CODES) {
            sreality.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("locality_region_id", equalTo(LARGE_REGION))
                    .withQueryParam("category_sub_cb", equalTo(String.valueOf(disposition)))
                    .withQueryParam("limit", equalTo("0")));
        }
        sreality.verify(0, getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("locality_region_id", matching("^(?!" + LARGE_REGION + "$).*"))
                .withQueryParam("category_sub_cb", matching(".*")));
        sreality.verify(0, getRequestedFor(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("locality_region_id", equalTo(LARGE_REGION))
                .withQueryParam("category_sub_cb", absent())
                .withQueryParam("limit", equalTo(String.valueOf(PAGE_SIZE))));
    }

    private static MappingBuilder search() {
        return get(urlPathEqualTo(SEARCH_PATH));
    }

    private static MappingBuilder region(String regionId) {
        return search().withQueryParam("locality_region_id", equalTo(regionId));
    }

    private static MappingBuilder disposition(String code) {
        return region(LARGE_REGION).withQueryParam("category_sub_cb", equalTo(code));
    }

    private static MappingBuilder page(MappingBuilder bucket, int offset) {
        return bucket.withQueryParam("limit", equalTo(String.valueOf(PAGE_SIZE)))
                .withQueryParam("offset", equalTo(String.valueOf(offset)));
    }

    private static ResponseDefinitionBuilder fixture(String name) {
        String path = "fixtures/sreality/" + name;
        try (InputStream in = SrealityIdCollectorTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s", path).isNotNull();
            return okJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
