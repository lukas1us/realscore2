package com.realscore.analysis.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.analysis.LocalityLevel;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class LocalityTest {

    private static final Set<String> LARGE_CITIES = Set.of("Praha");

    @Test
    void aCityPartOfALargeCityIsTheFinestLocality() {
        assertThat(Locality.finest("Hlavní město Praha", "Praha", "Praha", "Vinohrady", LARGE_CITIES))
                .isEqualTo(new Locality(LocalityLevel.CITY_PART, "Hlavní město Praha", "Praha", "Praha", "Vinohrady"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void aLargeCityWithoutACityPartIsTheCity(String cityPart) {
        assertThat(Locality.finest("Hlavní město Praha", "Praha", "Praha", cityPart, LARGE_CITIES))
                .isEqualTo(new Locality(LocalityLevel.CITY, "Hlavní město Praha", "Praha", "Praha", null));
    }

    @Test
    void aCityPartOutsideTheLargeCitiesIsIgnored() {
        assertThat(Locality.finest("Jihomoravský kraj", "Znojmo", "Znojmo", "Hradiště", LARGE_CITIES))
                .isEqualTo(new Locality(LocalityLevel.CITY, "Jihomoravský kraj", "Znojmo", "Znojmo", null));
    }

    @Test
    void missingFinerFieldsFallBackToDistrictOrRegion() {
        assertThat(Locality.finest("Kraj", "Okres", null, "Část", LARGE_CITIES))
                .isEqualTo(new Locality(LocalityLevel.DISTRICT, "Kraj", "Okres", null, null));
        assertThat(Locality.finest("Kraj", null, "Praha", "Část", LARGE_CITIES))
                .isEqualTo(new Locality(LocalityLevel.REGION, "Kraj", null, null, null));
        assertThat(Locality.finest(null, "Okres", "Praha", "Část", LARGE_CITIES)).isNull();
    }
}
