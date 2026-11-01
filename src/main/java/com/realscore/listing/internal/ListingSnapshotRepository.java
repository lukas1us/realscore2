package com.realscore.listing.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshot, Long> {

    Optional<ListingSnapshot> findTopByListingOrderByFetchedAtDesc(Listing listing);

    List<ListingSnapshot> findByListingOrderByFetchedAtAsc(Listing listing);

    long countByListing(Listing listing);

    /**
     * The {@code payload_hash} of a payload: SHA-256 of its jsonb text form (decision-8). Postgres normalises
     * whitespace and key order there, and the same expression can be used in SQL, as migration V10 does.
     */
    @Query(value = "SELECT encode(sha256(convert_to(CAST(:payload AS jsonb)::text, 'UTF8')), 'hex')",
            nativeQuery = true)
    String hashPayload(String payload);
}
