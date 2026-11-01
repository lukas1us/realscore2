package com.realscore.analysis.internal;

import com.realscore.analysis.LocalityLevel;
import java.util.Set;

/**
 * A place at one {@link LocalityLevel}, identified by its whole path so that same-named cities in different districts
 * stay apart. Fields below {@code level} are {@code null}.
 */
public record Locality(LocalityLevel level, String region, String district, String city, String cityPart) {

    /**
     * The finest locality the fields allow: the city part for one of {@code largeCities}, otherwise the city, falling
     * back to district or region when the finer fields are missing.
     *
     * @return {@code null} without a region, which leaves nothing to fall back to
     */
    public static Locality finest(String region, String district, String city, String cityPart,
            Set<String> largeCities) {
        if (region == null) {
            return null;
        }
        if (district == null) {
            return new Locality(LocalityLevel.REGION, region, null, null, null);
        }
        if (city == null) {
            return new Locality(LocalityLevel.DISTRICT, region, district, null, null);
        }
        if (cityPart != null && largeCities.contains(city)) {
            return new Locality(LocalityLevel.CITY_PART, region, district, city, cityPart);
        }
        return new Locality(LocalityLevel.CITY, region, district, city, null);
    }

    /** @return the enclosing locality one level up, or {@code null} for a region */
    public Locality parent() {
        return switch (level) {
            case CITY_PART -> new Locality(LocalityLevel.CITY, region, district, city, null);
            case CITY -> new Locality(LocalityLevel.DISTRICT, region, district, null, null);
            case DISTRICT -> new Locality(LocalityLevel.REGION, region, null, null, null);
            case REGION -> null;
        };
    }
}
