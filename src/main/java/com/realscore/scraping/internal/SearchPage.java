package com.realscore.scraping.internal;

import java.util.List;

/**
 * One page of a Sreality search.
 *
 * @param total how many estates match the search in all, not just on this page
 * @param estateIds the IDs on this page, in the order Sreality returned them
 */
public record SearchPage(long total, List<Long> estateIds) {
}
