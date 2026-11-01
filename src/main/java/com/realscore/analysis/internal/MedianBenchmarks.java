package com.realscore.analysis.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Medians of a value per (locality, disposition) with fallback: each sample counts towards its finest locality and
 * every enclosing one, and each finest locality takes the median of the first of those, walking outwards, that has at
 * least {@code minSampleSize} samples. A finest locality with no such group is left out.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MedianBenchmarks {

    /** One listing's value, placed at its finest locality. */
    public record Sample(Locality locality, int dispositionCode, String dispositionLabel, BigDecimal value) {
    }

    /**
     * @param locality the finest locality this benchmark is for
     * @param benchmarkLocality the locality whose samples the median was taken over: {@code locality} or an
     *                          enclosing one
     * @param median rounded to whole units
     * @param sampleSize how many samples {@code median} is based on
     */
    public record Benchmark(Locality locality, int dispositionCode, String dispositionLabel,
            Locality benchmarkLocality, BigDecimal median, int sampleSize) {
    }

    private record Group(Locality locality, int dispositionCode) {
    }

    /** @return one benchmark per (finest locality, disposition) with a large enough group, ordered by locality path */
    public static List<Benchmark> compute(List<Sample> samples, int minSampleSize) {
        Map<Group, List<BigDecimal>> valuesByGroup = new HashMap<>();
        Map<Group, String> finestGroups = new LinkedHashMap<>();
        for (Sample sample : samples) {
            finestGroups.putIfAbsent(new Group(sample.locality(), sample.dispositionCode()), sample.dispositionLabel());
            for (Locality l = sample.locality(); l != null; l = l.parent()) {
                valuesByGroup.computeIfAbsent(new Group(l, sample.dispositionCode()), k -> new ArrayList<>())
                        .add(sample.value());
            }
        }
        List<Benchmark> benchmarks = new ArrayList<>();
        finestGroups.forEach((finest, label) -> {
            for (Locality l = finest.locality(); l != null; l = l.parent()) {
                List<BigDecimal> values = valuesByGroup.get(new Group(l, finest.dispositionCode()));
                if (values.size() >= minSampleSize) {
                    benchmarks.add(new Benchmark(finest.locality(), finest.dispositionCode(), label, l,
                            median(values), values.size()));
                    return;
                }
            }
        });
        benchmarks.sort(ORDER);
        return benchmarks;
    }

    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        BigDecimal median = sorted.size() % 2 == 1
                ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.TWO);
        return median.setScale(0, RoundingMode.HALF_UP);
    }

    private static final Comparator<String> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());

    private static final Comparator<Benchmark> ORDER = Comparator
            .comparing((Benchmark b) -> b.locality().region(), NULLS_FIRST)
            .thenComparing(b -> b.locality().district(), NULLS_FIRST)
            .thenComparing(b -> b.locality().city(), NULLS_FIRST)
            .thenComparing(b -> b.locality().cityPart(), NULLS_FIRST)
            .thenComparingInt(Benchmark::dispositionCode);
}
