package com.realscore.parsing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Every parsed field of a listing's latest snapshot. Any field may be {@code null} when the snapshot did not carry
 * it. Codes are Sreality's raw values (doc "Sreality API"): transaction type 1 = sale, 2 = rent; property category
 * 1 = flats. {@code floor} is Sreality's {@code floor_number}, the Czech podlaží (NP) number: 1 is the ground floor,
 * 2 the first floor above it (1. patro), 0 ground or raised ground as some sellers enter it, negative underground.
 * {@code url} is the public Sreality detail page.
 */
public record ListingDetails(
        BigDecimal priceCzk,
        BigDecimal sizeM2,
        Integer dispositionCode,
        String dispositionLabel,
        Integer acceptanceYear,
        Integer reconstructionYear,
        Integer floor,
        String energyClass,
        String constructionType,
        Boolean elevator,
        String ownership,
        Integer buildingConditionCode,
        String buildingConditionLabel,
        String city,
        String district,
        String region,
        String citypart,
        Integer transactionTypeCode,
        Integer propertyCategoryCode,
        String url,
        Instant parsedAt) {
}
