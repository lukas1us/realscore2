package com.realscore.parsing;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Read access to parsed listing details for other modules. Joins listing_detail with its listing to leave out
 * listings marked removed.
 */
@Service
@RequiredArgsConstructor
public class ListingDetailsQueryService {

    private final JdbcClient jdbc;

    /**
     * @param transactionTypeCode Sreality {@code category_type_cb} value (1 = sale, 2 = rent)
     * @param propertyCategoryCode Sreality {@code category_main_cb} value (1 = flats)
     * @return the details of every active listing of that transaction type and property category, in no order
     */
    public List<ActiveListingDetails> findActive(int transactionTypeCode, int propertyCategoryCode) {
        return jdbc.sql("""
                        SELECT d.listing_id, d.price_czk, d.size_m2, d.disposition_code, d.disposition_label,
                               d.region, d.district, d.city, d.citypart
                        FROM listing_detail d JOIN listing l ON l.id = d.listing_id
                        WHERE l.removed_at IS NULL
                          AND d.transaction_type_code = :transactionType
                          AND d.property_category_code = :propertyCategory
                        """)
                .param("transactionType", transactionTypeCode)
                .param("propertyCategory", propertyCategoryCode)
                .query(ActiveListingDetails.class)
                .list();
    }
}
