package com.realscore.parsing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One asking price of a listing, from the first snapshot (by fetch time) that carried it. */
@Entity
@Table(name = "listing_price_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingPricePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "price_czk", nullable = false)
    private BigDecimal priceCzk;

    public ListingPricePoint(Long listingId, Instant observedAt, BigDecimal priceCzk) {
        this.listingId = listingId;
        this.observedAt = observedAt;
        this.priceCzk = priceCzk;
    }
}
