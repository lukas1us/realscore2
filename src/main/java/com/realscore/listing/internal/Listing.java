package com.realscore.listing.internal;

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
import lombok.Setter;

@Entity
@Table(name = "listing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_fetched_at", nullable = false)
    private Instant lastFetchedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    /** Raw {@code price_summary_czk} of the last search result taken into account; 0 is price on request. */
    @Column(name = "search_price_czk")
    private BigDecimal searchPriceCzk;

    /**
     * When the removal check first found the listing still online although the search no longer returned it (e.g.
     * above the price cap); {@code null} while in scope. Cleared when the search returns it again.
     */
    @Column(name = "out_of_scope_since")
    private Instant outOfScopeSince;

    public Listing(String source, String externalId, Instant seenAt) {
        this.source = source;
        this.externalId = externalId;
        this.firstSeenAt = seenAt;
        this.lastSeenAt = seenAt;
        this.lastFetchedAt = seenAt;
    }
}
