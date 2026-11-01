package com.realscore.parsing;

/**
 * Counts from one {@link ListingDetailsReparseService} run, by {@link ParseOutcome}.
 *
 * @param failed listings whose snapshot could not be parsed or stored; their listing_detail row is left as it was
 */
public record ReparseResult(int created, int updated, int unchanged, int failed) {
}
