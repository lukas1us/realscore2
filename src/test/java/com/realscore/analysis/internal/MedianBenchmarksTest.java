package com.realscore.analysis.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.realscore.analysis.LocalityLevel;
import com.realscore.analysis.internal.MedianBenchmarks.Benchmark;
import com.realscore.analysis.internal.MedianBenchmarks.Sample;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MedianBenchmarksTest {

    private static final int DISPOSITION_2KK = 4;
    private static final Set<String> LARGE_CITIES = Set.of("Praha");

    private static final Locality SMICHOV = Locality.finest("Praha", "Praha 5", "Praha", "Smíchov", LARGE_CITIES);
    private static final Locality KOSIRE = Locality.finest("Praha", "Praha 5", "Praha", "Košíře", LARGE_CITIES);
    private static final Locality PLZEN = Locality.finest("Plzeňský kraj", "Plzeň-město", "Plzeň", "Slovany",
            LARGE_CITIES);
    private static final Locality STARY_PLZENEC = Locality.finest("Plzeňský kraj", "Plzeň-město", "Starý Plzenec",
            null, LARGE_CITIES);
    private static final Locality ROKYCANY = Locality.finest("Plzeňský kraj", "Rokycany", "Rokycany", null,
            LARGE_CITIES);

    @Test
    void largeCitiesAreSplitByCityPartAndOtherCitiesAreNot() {
        assertThat(SMICHOV.level()).isEqualTo(LocalityLevel.CITY_PART);
        assertThat(PLZEN).isEqualTo(new Locality(LocalityLevel.CITY, "Plzeňský kraj", "Plzeň-město", "Plzeň", null));
        assertThat(Locality.finest("Praha", "Praha 5", "Praha", null, LARGE_CITIES).level())
                .isEqualTo(LocalityLevel.CITY);
        assertThat(Locality.finest("Kraj", "Okres", null, null, LARGE_CITIES).level())
                .isEqualTo(LocalityLevel.DISTRICT);
        assertThat(Locality.finest(null, "Okres", "Město", null, LARGE_CITIES)).isNull();
    }

    @Test
    void medianOfOddAndEvenCounts() {
        assertThat(MedianBenchmarks.median(values(3, 1, 2))).isEqualByComparingTo("2");
        assertThat(MedianBenchmarks.median(values(4, 1, 2, 3))).isEqualByComparingTo("3");
        assertThat(MedianBenchmarks.median(List.of(new BigDecimal("1.4")))).isEqualByComparingTo("1");
    }

    @Test
    void aLargeEnoughLocalityUsesItsOwnMedian() {
        List<Benchmark> benchmarks = MedianBenchmarks.compute(samples(PLZEN, 100, 200, 300), 3);

        assertThat(benchmarks).containsExactly(
                new Benchmark(PLZEN, DISPOSITION_2KK, "2+kk", PLZEN, new BigDecimal("200"), 3));
    }

    @Test
    void aSmallCityPartFallsBackToItsCity() {
        List<Sample> samples = new ArrayList<>(samples(SMICHOV, 100, 200));
        samples.addAll(samples(KOSIRE, 400));

        List<Benchmark> benchmarks = MedianBenchmarks.compute(samples, 3);

        assertThat(benchmarks).hasSize(2).allSatisfy(b -> {
            assertThat(b.benchmarkLocality()).isEqualTo(SMICHOV.parent());
            assertThat(b.median()).isEqualByComparingTo("200");
            assertThat(b.sampleSize()).isEqualTo(3);
        });
    }

    @Test
    void aSmallCityFallsBackToItsDistrictThenItsRegion() {
        List<Sample> samples = new ArrayList<>(samples(PLZEN, 100, 200));
        samples.addAll(samples(STARY_PLZENEC, 300));
        samples.addAll(samples(ROKYCANY, 1000));

        List<Benchmark> benchmarks = MedianBenchmarks.compute(samples, 3);

        assertThat(benchmarks).extracting(b -> b.locality().city(), b -> b.benchmarkLocality().level(),
                        Benchmark::median, Benchmark::sampleSize)
                .containsExactly(
                        tuple("Plzeň", LocalityLevel.DISTRICT, new BigDecimal("200"), 3),
                        tuple("Starý Plzenec", LocalityLevel.DISTRICT,
                                new BigDecimal("200"), 3),
                        tuple("Rokycany", LocalityLevel.REGION, new BigDecimal("250"),
                                4));
    }

    @Test
    void groupsTooSmallEvenAtRegionLevelAreLeftOut() {
        assertThat(MedianBenchmarks.compute(samples(ROKYCANY, 100, 200), 3)).isEmpty();
    }

    @Test
    void dispositionsAreBenchmarkedSeparately() {
        List<Sample> samples = new ArrayList<>(samples(PLZEN, 100, 200, 300));
        samples.add(new Sample(PLZEN, 6, "3+kk", new BigDecimal("999")));

        assertThat(MedianBenchmarks.compute(samples, 3))
                .extracting(Benchmark::dispositionCode)
                .containsExactly(DISPOSITION_2KK);
    }

    private static List<Sample> samples(Locality locality, int... values) {
        return values(values).stream().map(v -> new Sample(locality, DISPOSITION_2KK, "2+kk", v)).toList();
    }

    private static List<BigDecimal> values(int... values) {
        return Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }
}
