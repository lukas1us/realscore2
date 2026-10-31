package com.realscore.scraping.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.realscore.PostgresContainerConfig;
import com.realscore.scraping.ScrapeRunStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
    void failedRunIsClosedAsFailedWithTheCause() {
        stubSearch(aResponse().withStatus(500));

        job.run();

        Map<String, Object> run = latestRun();
        assertThat(run.get("status")).isEqualTo(ScrapeRunStatus.FAILED.name());
        assertThat(run.get("finished_at")).isNotNull();
        assertThat((String) run.get("error_details")).contains("SrealityApiException").contains("HTTP 500");
    }

    /** Region 1 answers with {@code region1}; every other region is empty. */
    private static void stubSearch(ResponseDefinitionBuilder region1) {
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH)).atPriority(10)
                .willReturn(okJson(fixture("search_count_empty.json"))));
        sreality.stubFor(get(urlPathEqualTo(SrealityTestSupport.SEARCH_PATH))
                .withQueryParam("locality_region_id", equalTo("1"))
                .willReturn(region1));
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
