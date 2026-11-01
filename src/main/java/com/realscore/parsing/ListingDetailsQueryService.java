package com.realscore.parsing;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.parsing.internal.ListingDetail;
import com.realscore.parsing.internal.ListingDetailRepository;
import com.realscore.parsing.internal.ListingPricePointRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to parsed listing details for other modules: all active listings for market analysis, recent price
 * histories, or one listing with its price history.
 */
@Service
@RequiredArgsConstructor
public class ListingDetailsQueryService {

    private final JdbcClient jdbc;
    private final ListingSnapshotService listingSnapshotService;
    private final ListingDetailRepository listingDetailRepository;
    private final ListingPricePointRepository pricePointRepository;

    /** @return the listing, removed or not, or empty if no listing from {@code source} has that external ID */
    @Transactional(readOnly = true)
    public Optional<ListingOverview> findListing(String source, String externalId) {
        return listingSnapshotService.findListing(source, externalId).map(listing -> new ListingOverview(
                source,
                externalId,
                listing.firstSeenAt(),
                listing.lastSeenAt(),
                listing.removedAt(),
                listingDetailRepository.findByListingId(listing.listingId())
                        .map(ListingDetailsQueryService::toDetails)
                        .orElse(null),
                pricePointRepository.findByListingIdOrderByObservedAtAsc(listing.listingId()).stream()
                        .map(point -> new PricePoint(point.getObservedAt(), point.getPriceCzk()))
                        .toList()));
    }

    /**
     * Joins listing_detail with its listing to leave out listings marked removed.
     *
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

    /**
     * @param since only listings with a price point observed at or after this time are returned
     * @param transactionTypeCode Sreality {@code category_type_cb} value (1 = sale, 2 = rent), or {@code null} for both
     * @return the full price history of every matching active listing, in no particular order of listings
     */
    @Transactional(readOnly = true)
    public List<ActivePriceHistory> findActivePriceHistoriesChangedSince(Instant since, Integer transactionTypeCode) {
        Map<Long, ActivePriceHistory> histories = new LinkedHashMap<>();
        jdbc.sql("""
                        SELECT l.id, l.source, l.external_id, d.transaction_type_code, d.disposition_label, d.size_m2,
                               d.city, d.citypart, p.observed_at, p.price_czk
                        FROM listing l
                        JOIN listing_detail d ON d.listing_id = l.id
                        JOIN listing_price_point p ON p.listing_id = l.id
                        WHERE l.removed_at IS NULL
                          AND (CAST(:transactionType AS INTEGER) IS NULL OR d.transaction_type_code = :transactionType)
                          AND EXISTS (SELECT 1 FROM listing_price_point recent
                                      WHERE recent.listing_id = l.id AND recent.observed_at >= :since)
                        ORDER BY l.id, p.observed_at
                        """)
                .param("transactionType", transactionTypeCode)
                .param("since", OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
                .query(rs -> {
                    Long listingId = rs.getLong("id");
                    ActivePriceHistory history = histories.get(listingId);
                    if (history == null) {
                        history = new ActivePriceHistory(rs.getString("source"), rs.getString("external_id"),
                                rs.getObject("transaction_type_code", Integer.class), rs.getString("disposition_label"),
                                rs.getBigDecimal("size_m2"), rs.getString("city"), rs.getString("citypart"),
                                new ArrayList<>());
                        histories.put(listingId, history);
                    }
                    history.priceHistory().add(new PricePoint(
                            rs.getObject("observed_at", OffsetDateTime.class).toInstant(),
                            rs.getBigDecimal("price_czk")));
                });
        return List.copyOf(histories.values());
    }

    private static ListingDetails toDetails(ListingDetail d) {
        return new ListingDetails(d.getPriceCzk(), d.getSizeM2(), d.getDispositionCode(), d.getDispositionLabel(),
                d.getYearBuilt(), d.getFloor(), d.getEnergyClass(), d.getConstructionType(), d.getElevator(),
                d.getOwnership(), d.getCity(), d.getDistrict(), d.getRegion(), d.getCitypart(),
                d.getTransactionTypeCode(), d.getPropertyCategoryCode(), d.getParsedAt());
    }
}
