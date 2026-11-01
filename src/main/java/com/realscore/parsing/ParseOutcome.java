package com.realscore.parsing;

/**
 * What {@link ListingDetailsParsingService#parse} did with a listing's listing_detail row.
 */
public enum ParseOutcome {
    /** The listing had no listing_detail row yet; one was written. */
    CREATED,
    /** The parsed fields differ from the stored ones; the row was overwritten. */
    UPDATED,
    /** The parsed fields equal the stored ones; the row was left untouched. */
    UNCHANGED
}
