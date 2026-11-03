package com.realscore.parsing.internal;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * Builds the public Sreality detail page URL from the {@code result} of a v1 detail response (doc "Sreality API"):
 * {@code https://www.sreality.cz/detail/{type}/{main}/{sub}/{locality}/{hash_id}}. Sreality does not resolve a
 * detail page by ID alone (/detail/-/-/-/-/{id} is a 404), so every segment must be right or no URL is built.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SrealityListingUrl {

    private static final String BASE = "https://www.sreality.cz/detail/";

    private static final Map<Integer, String> TYPES = Map.of(1, "prodej", 2, "pronajem");

    private static final Map<Integer, String> MAINS = Map.of(
            1, "byt", 2, "dum", 3, "pozemek", 4, "komercni", 5, "ostatni");

    private static final String[] LOCALITY_PARTS = {"city_seo_name", "citypart_seo_name", "street_seo_name"};

    /** @return the URL, or {@code null} if the type or main code is unknown or sub, locality or hash_id is missing */
    static String from(JsonNode result) {
        String type = segment(TYPES, result.path("category_type_cb"));
        String main = segment(MAINS, result.path("category_main_cb"));
        String sub = slug(textOrBlank(result.path("category_sub_cb").path("name")));
        String locality = locality(result.path("locality"));
        JsonNode hashId = result.path("hash_id");
        if (type == null || main == null || sub.isEmpty() || locality.isEmpty() || !hashId.isIntegralNumber()) {
            return null;
        }
        return BASE + String.join("/", type, main, sub, locality, hashId.asString());
    }

    /** City, city part and street SEO names joined with "-", blank ones skipped and exact repeats dropped. */
    private static String locality(JsonNode locality) {
        Set<String> parts = new LinkedHashSet<>();
        Stream.of(LOCALITY_PARTS)
                .map(field -> textOrBlank(locality.path(field)).strip())
                .filter(part -> !part.isEmpty())
                .forEach(parts::add);
        return String.join("-", parts);
    }

    /**
     * Lowercase, accents stripped, whitespace runs turned into one "-". Confirmed on the site for every flat layout
     * (e.g. "1+kk", "6 a více" → "6-a-vice", "Atypický" → "atypicky"); other house and land subtypes are unverified
     * (doc "Sreality API").
     */
    private static String slug(String name) {
        String withoutAccents = Normalizer.normalize(name.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    /** Immutable maps reject a null key, so a missing code is checked before the lookup. */
    private static String segment(Map<Integer, String> segments, JsonNode codeAndName) {
        JsonNode value = codeAndName.path("value");
        return value.isNumber() ? segments.get(value.intValue()) : null;
    }

    private static String textOrBlank(JsonNode value) {
        return value.isString() ? value.stringValue() : "";
    }
}
