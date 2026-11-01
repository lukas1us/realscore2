package com.realscore.scraping.internal;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Removes the seller's contact data from a Sreality detail response before it is stored (decision-8): the broker
 * ({@code result.user}: name, email, phones) and the agency's web URLs ({@code result.premise.web_url},
 * {@code result.premise.web_visible_url}). Everything else is kept as received.
 */
final class SrealityPayloadScrubber {

    /** Reads decimals as {@code BigDecimal}, so numbers are written back exactly as they came. */
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private SrealityPayloadScrubber() {
    }

    /**
     * @return the payload without the contact fields, or {@code payload} itself when it has none of them
     * @throws IllegalStateException if the payload is not valid JSON
     */
    static String scrub(String payload) {
        JsonNode root;
        try {
            root = MAPPER.readTree(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Sreality returned a body that is not valid JSON", e);
        }
        if (!(root.path("result") instanceof ObjectNode result)) {
            return payload;
        }
        boolean removed = result.remove("user") != null;
        if (result.path("premise") instanceof ObjectNode premise) {
            removed |= premise.remove("web_url") != null;
            removed |= premise.remove("web_visible_url") != null;
        }
        return removed ? MAPPER.writeValueAsString(root) : payload;
    }
}
