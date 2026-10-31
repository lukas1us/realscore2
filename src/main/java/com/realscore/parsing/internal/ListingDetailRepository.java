package com.realscore.parsing.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingDetailRepository extends JpaRepository<ListingDetail, Long> {

    Optional<ListingDetail> findByListingId(Long listingId);
}
