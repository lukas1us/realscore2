package com.realscore.parsing;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.parsing.internal.ListingDetail;
import com.realscore.parsing.internal.ListingDetailRepository;
import com.realscore.parsing.internal.ListingPricePointRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to parsed listing details for other modules: all active listings for market analysis, or one listing
 * with its price history.
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

    private static ListingDetails toDetails(ListingDetail d) {
        return new ListingDetails(d.getPriceCzk(), d.getSizeM2(), d.getDispositionCode(), d.getDispositionLabel(),
                d.getYearBuilt(), d.getFloor(), d.getEnergyClass(), d.getConstructionType(), d.getElevator(),
                d.getOwnership(), d.getCity(), d.getDistrict(), d.getRegion(), d.getCitypart(),
                d.getTransactionTypeCode(), d.getPropertyCategoryCode(), d.getParsedAt());
    }
}
