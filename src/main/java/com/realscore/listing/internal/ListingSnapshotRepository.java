package com.realscore.listing.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshot, Long> {

    Optional<ListingSnapshot> findTopByListingOrderByFetchedAtDesc(Listing listing);

    long countByListing(Listing listing);
}
