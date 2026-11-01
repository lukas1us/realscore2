package com.realscore.scraping.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SrealityPayloadScrubberTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void removesTheBrokerAndTheAgencyWebUrlsAndKeepsEverythingElse() {
        String payload = """
                {"result": {
                  "price_summary_czk": 3590000.0,
                  "locality": {"gps_lat": 49.429272123456789},
                  "user": {"user_name": "Jan", "user_email": "jan@example.com", "user_phones": [{"phone": "1"}]},
                  "premise": {"name": "Agency", "web_url": "https://a.example", "web_visible_url": "a.example"}
                }, "status_code": 200}
                """;

        String scrubbed = SrealityPayloadScrubber.scrub(payload);

        assertThat(scrubbed).doesNotContain("user_email", "user_phones", "jan@example.com", "web_url", "a.example");
        assertThat(MAPPER.readTree(scrubbed)).isEqualTo(MAPPER.readTree("""
                {"result": {
                  "price_summary_czk": 3590000.0,
                  "locality": {"gps_lat": 49.429272123456789},
                  "premise": {"name": "Agency"}
                }, "status_code": 200}
                """));
        assertThat(scrubbed).contains("3590000.0").contains("49.429272123456789");
    }

    @Test
    void aNullUserIsRemovedToo() {
        JsonNode scrubbed = MAPPER.readTree(SrealityPayloadScrubber.scrub("{\"result\": {\"user\": null, \"x\": 1}}"));

        assertThat(scrubbed).isEqualTo(MAPPER.readTree("{\"result\": {\"x\": 1}}"));
    }

    @Test
    void aPayloadWithoutContactDataIsReturnedUnchanged() {
        String payload = "{\"result\": {\"premise\": {\"name\": \"Agency\"}},   \"status_code\": 200}";

        assertThat(SrealityPayloadScrubber.scrub(payload)).isSameAs(payload);
        assertThat(SrealityPayloadScrubber.scrub("{\"result\": null}")).isEqualTo("{\"result\": null}");
    }

    @Test
    void invalidJsonFailsAsIllegalState() {
        assertThatThrownBy(() -> SrealityPayloadScrubber.scrub("{not json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid JSON");
    }
}
