package com.realscore.listing.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findBySourceAndExternalId(String source, String externalId);
}
