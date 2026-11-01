package com.realscore.parsing.internal;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Extracts normalized listing-detail fields from a raw Sreality v1 detail response
 * (see Backlog doc "Sreality API"). Every field is read directly from the response -
 * v1 has no {@code items} list, so there is no fallback-priority logic to apply.
 */
@Component
@RequiredArgsConstructor
public class SrealityListingDetailsExtractor {

    private final ObjectMapper objectMapper;

    public ListingDetailsFields extract(String rawPayload) {
        JsonNode result = parse(rawPayload).path("result");

        return new ListingDetailsFields(
                extractPrice(result),
                decimalOrNull(result, "usable_area", "floor_area"),
                intOrNull(result.path("category_sub_cb"), "value"),
                textOrNull(result.path("category_sub_cb"), "name"),
                yearBuilt(result),
                intOrNull(result, "floor_number"),
                energyClass(result),
                textOrNull(result.path("building_type"), "name"),
                elevator(result),
                ownership(result),
                textOrNull(result.path("locality"), "city"),
                textOrNull(result.path("locality"), "district"),
                textOrNull(result.path("locality"), "region"),
                textOrNull(result.path("locality"), "citypart"),
                intOrNull(result.path("category_type_cb"), "value"),
                intOrNull(result.path("category_main_cb"), "value"));
    }

    private JsonNode parse(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not valid JSON", e);
        }
    }

    /**
     * price_summary_czk is the authoritative total price; price_czk can be a per-unit
     * rate instead. 0 (or missing) means "price on request" - no error, no price.
     */
    private static BigDecimal extractPrice(JsonNode result) {
        JsonNode price = result.path("price_summary_czk");
        if (!price.isNumber() || price.decimalValue().signum() == 0) {
            return null;
        }
        return price.decimalValue();
    }

    private static Integer yearBuilt(JsonNode result) {
        Integer acceptanceYear = intOrNull(result, "acceptance_year");
        return acceptanceYear != null ? acceptanceYear : intOrNull(result, "reconstruction_year");
    }

    /**
     * Mapped from the code, not the name text: 1..7 are classes A..G. 0 ("- vyber třídu")
     * means the seller chose no class, so it is unknown, like any other code.
     */
    private static String energyClass(JsonNode result) {
        Integer value = intOrNull(result.path("energy_efficiency_rating_cb"), "value");
        if (value == null || value < 1 || value > 7) {
            return null;
        }
        return String.valueOf((char) ('A' + value - 1));
    }

    /**
     * 1 is "Ano", 2 is "Ne". 0 ("- nezadáno") means the seller left it empty, so it is
     * unknown, like any other code.
     */
    private static Boolean elevator(JsonNode result) {
        Integer value = intOrNull(result.path("elevator"), "value");
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> true;
            case 2 -> false;
            default -> null;
        };
    }

    private static String ownership(JsonNode result) {
        Integer value = intOrNull(result.path("ownership"), "value");
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> "OV";
            case 2 -> "DV";
            default -> "OTHER";
        };
    }

    private static BigDecimal decimalOrNull(JsonNode result, String primaryField, String fallbackField) {
        JsonNode primary = result.path(primaryField);
        if (primary.isNumber()) {
            return primary.decimalValue();
        }
        JsonNode fallback = result.path(fallbackField);
        return fallback.isNumber() ? fallback.decimalValue() : null;
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.intValue() : null;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.stringValue() : null;
    }
}
