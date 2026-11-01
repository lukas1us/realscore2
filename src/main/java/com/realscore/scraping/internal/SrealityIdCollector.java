package com.realscore.scraping.internal;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Collects the IDs of all Sreality apartments for sale or for rent, optionally under a price cap, by splitting the
 * search into buckets: one per region, and for regions over the bucket threshold, one per (region, disposition).
 * See Backlog doc "Sreality API".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SrealityIdCollector {

    static final List<Integer> REGION_IDS = IntStream.rangeClosed(1, 14).boxed().toList();
    static final List<Integer> DISPOSITION_CODES = IntStream.rangeClosed(2, 12).boxed().toList();
    private static final String APARTMENTS = "1";
    private static final String SALE = "1";
    private static final String RENT = "2";

    private final SrealityClient client;
    private final SrealityProperties properties;

    /**
     * @param priceCap maximum price in CZK ({@code price_to})
     * @return the IDs of every matching estate, each once, in the order first seen
     * @throws SrealityApiException if any Sreality request fails; no partial result is returned then
     */
    public Set<Long> collectSaleApartmentIds(long priceCap) {
        return collect(apartments(SALE, priceCap));
    }

    /**
     * @param priceCap maximum monthly rent in CZK ({@code price_to}), or {@code null} for no cap
     * @return the IDs of every matching estate, each once, in the order first seen
     * @throws SrealityApiException if any Sreality request fails; no partial result is returned then
     */
    public Set<Long> collectRentalApartmentIds(Long priceCap) {
        return collect(apartments(RENT, priceCap));
    }

    private static Map<String, String> apartments(String categoryType, Long priceCap) {
        Map<String, String> params = new HashMap<>();
        params.put("category_main_cb", APARTMENTS);
        params.put("category_type_cb", categoryType);
        if (priceCap != null) {
            params.put("price_to", String.valueOf(priceCap));
        }
        return Map.copyOf(params);
    }

    private Set<Long> collect(Map<String, String> filters) {
        Set<Long> estateIds = new LinkedHashSet<>();
        for (int regionId : REGION_IDS) {
            Map<String, String> region = with(filters, "locality_region_id", regionId);
            long regionCount = client.countEstates(region);
            if (regionCount <= properties.bucketThreshold()) {
                collectBucket(region, regionCount, estateIds);
            } else {
                for (int disposition : DISPOSITION_CODES) {
                    Map<String, String> bucket = with(region, "category_sub_cb", disposition);
                    long bucketCount = client.countEstates(bucket);
                    if (bucketCount > properties.bucketThreshold()) {
                        log.warn("Bucket {} has {} estates, over the threshold of {}; collecting it anyway",
                                bucket, bucketCount, properties.bucketThreshold());
                    }
                    collectBucket(bucket, bucketCount, estateIds);
                }
            }
        }
        return estateIds;
    }

    private void collectBucket(Map<String, String> bucket, long count, Set<Long> estateIds) {
        long total = count;
        int offset = 0;
        while (offset < total) {
            SearchPage page = client.searchEstates(bucket, properties.pageSize(), offset);
            if (page.estateIds().isEmpty()) {
                break;
            }
            estateIds.addAll(page.estateIds());
            total = page.total();
            offset += page.estateIds().size();
        }
    }

    private static Map<String, String> with(Map<String, String> params, String key, int value) {
        Map<String, String> result = new HashMap<>(params);
        result.put(key, String.valueOf(value));
        return Map.copyOf(result);
    }
}
