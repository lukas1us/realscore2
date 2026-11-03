package com.realscore.analysis;

import java.math.BigDecimal;

/**
 * How good a deal an active flat for sale is, 0-100, with the parts it is made of.
 *
 * @param url the public Sreality detail page; {@code null} if the snapshot lacked a part it needs
 * @param total weighted average of the parts that have data, rounded to 1 decimal
 * @param discount how much cheaper per m² than the local sale median; {@code null} without a sale benchmark
 * @param rentalYield gross yearly rent from the local rent medians over the price; {@code null} without a rent
 *                    benchmark
 * @param priceDrop drop over the price-drop window, 0 without one
 * @param quality the flat's own quality factors (TASK-35), always present; 50 when none is known
 */
public record ListingScore(
        String source,
        String externalId,
        String dispositionLabel,
        String region,
        String district,
        String city,
        String citypart,
        String url,
        BigDecimal priceCzk,
        BigDecimal sizeM2,
        BigDecimal total,
        ScorePart discount,
        ScorePart rentalYield,
        ScorePart priceDrop,
        QualityScorePart quality) {
}
