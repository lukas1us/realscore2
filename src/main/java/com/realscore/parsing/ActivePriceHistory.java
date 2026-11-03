package com.realscore.parsing;

import java.math.BigDecimal;
import java.util.List;

/**
 * An active listing's price history, oldest first, with the parsed fields needed to tell listings apart in a list.
 * Any of those fields may be {@code null} when the listing's snapshot did not carry it.
 *
 * @param transactionTypeCode Sreality {@code category_type_cb} value (1 = sale, 2 = rent)
 */
public record ActivePriceHistory(
        String source,
        String externalId,
        Integer transactionTypeCode,
        String dispositionLabel,
        BigDecimal sizeM2,
        String city,
        String citypart,
        String url,
        List<PricePoint> priceHistory) {
}
