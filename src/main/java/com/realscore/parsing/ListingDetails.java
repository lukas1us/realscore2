package com.realscore.parsing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Every parsed field of a listing's latest snapshot. Any field may be {@code null} when the snapshot did not carry
 * it. Codes are Sreality's raw values (doc "Sreality API"): transaction type 1 = sale, 2 = rent; property category
 * 1 = flats.
 */
public record ListingDetails(
        BigDecimal priceCzk,
        BigDecimal sizeM2,
        Integer dispositionCode,
        String dispositionLabel,
        Integer yearBuilt,
        Integer floor,
        String energyClass,
        String constructionType,
        Boolean elevator,
        String ownership,
        String city,
        String district,
        String region,
        String citypart,
        Integer transactionTypeCode,
        Integer propertyCategoryCode,
        Instant parsedAt) {
}
