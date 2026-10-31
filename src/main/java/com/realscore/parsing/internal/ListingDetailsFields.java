package com.realscore.parsing.internal;

import java.math.BigDecimal;

public record ListingDetailsFields(
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
        String region) {
}
