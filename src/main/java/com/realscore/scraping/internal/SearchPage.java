package com.realscore.scraping.internal;

import java.math.BigDecimal;
import java.util.List;

/**
 * One page of a Sreality search.
 *
 * @param total how many estates match the search in all, not just on this page
 * @param results the estates on this page, in the order Sreality returned them
 */
public record SearchPage(long total, List<Result> results) {

    /**
     * @param searchPriceCzk raw {@code price_summary_czk}: 0 for price on request, {@code null} if the result has
     *                       none
     */
    public record Result(long estateId, BigDecimal searchPriceCzk) {
    }

    public List<Long> estateIds() {
        return results.stream().map(Result::estateId).toList();
    }
}
