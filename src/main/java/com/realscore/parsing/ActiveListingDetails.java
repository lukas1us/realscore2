package com.realscore.parsing;

import java.math.BigDecimal;

/**
 * The parsed details of a listing that is still active (not marked removed), as far as market analysis needs them.
 * Any field may be {@code null} when the listing's snapshot did not carry it.
 */
public record ActiveListingDetails(
        Long listingId,
        String source,
        String externalId,
        BigDecimal priceCzk,
        BigDecimal sizeM2,
        Integer dispositionCode,
        String dispositionLabel,
        String region,
        String district,
        String city,
        String citypart,
        Integer floor,
        String energyClass,
        String constructionType,
        Boolean elevator,
        String ownership,
        Integer buildingConditionCode,
        String buildingConditionLabel,
        String url) {
}
