package com.realscore.parsing.internal;

import java.math.BigDecimal;
import lombok.With;

@With
public record ListingDetailsFields(
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
        String url) {
}
