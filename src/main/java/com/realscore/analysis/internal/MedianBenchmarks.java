package com.realscore.analysis.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

    /**
     * The medians of every (locality, disposition) group the samples fall into, at every level, for looking up the
     * benchmark of any locality: also one with no samples of its own, which then falls back to an enclosing one.
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Index {

        private record Median(BigDecimal value, int sampleSize) {
        }

        private final Map<Group, Median> mediansByGroup;
        private final Map<Integer, String> labelsByDisposition;
        private final int minSampleSize;

        /**
         * @return the median of the first group, walking outwards from {@code locality}, with at least
         *     {@code minSampleSize} samples; empty if there is none even at region level
         */
        public Optional<Benchmark> find(Locality locality, int dispositionCode) {
            for (Locality l = locality; l != null; l = l.parent()) {
                Median median = mediansByGroup.get(new Group(l, dispositionCode));
                if (median != null && median.sampleSize() >= minSampleSize) {
                    return Optional.of(new Benchmark(locality, dispositionCode,
                            labelsByDisposition.get(dispositionCode), l, median.value(), median.sampleSize()));
                }
            }
            return Optional.empty();
        }
    }

    /** Groups each sample under its finest locality and every enclosing one, and takes each group's median. */
    public static Index index(List<Sample> samples, int minSampleSize) {
        Map<Group, List<BigDecimal>> valuesByGroup = new HashMap<>();
        Map<Integer, String> labels = new HashMap<>();
        for (Sample sample : samples) {
            labels.putIfAbsent(sample.dispositionCode(), sample.dispositionLabel());
            for (Locality l = sample.locality(); l != null; l = l.parent()) {
                valuesByGroup.computeIfAbsent(new Group(l, sample.dispositionCode()), k -> new ArrayList<>())
                        .add(sample.value());
            }
        }
        Map<Group, Index.Median> medians = new HashMap<>();
        valuesByGroup.forEach((group, values) -> medians.put(group, new Index.Median(median(values), values.size())));
        return new Index(medians, labels, minSampleSize);
    }

    /** @return one benchmark per (finest locality, disposition) with a large enough group, ordered by locality path */
    public static List<Benchmark> compute(List<Sample> samples, int minSampleSize) {
        Index index = index(samples, minSampleSize);
        Map<Group, String> finestGroups = new LinkedHashMap<>();
        for (Sample sample : samples) {
            finestGroups.putIfAbsent(new Group(sample.locality(), sample.dispositionCode()), sample.dispositionLabel());
        }
        List<Benchmark> benchmarks = new ArrayList<>();
        finestGroups.forEach((finest, label) -> index.find(finest.locality(), finest.dispositionCode())
                .map(b -> new Benchmark(b.locality(), b.dispositionCode(), label, b.benchmarkLocality(), b.median(),
                        b.sampleSize()))
                .ifPresent(benchmarks::add));
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
