package com.realscore.parsing.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingPricePointRepository extends JpaRepository<ListingPricePoint, Long> {

    List<ListingPricePoint> findByListingIdOrderByObservedAtAsc(Long listingId);

    /**
     * A bulk delete that runs immediately. A derived {@code deleteBy...} would be queued by Hibernate behind the
     * inserts of the replacement points and hit the (listing_id, observed_at) unique constraint.
     */
    @Modifying
    @Query("DELETE FROM ListingPricePoint p WHERE p.listingId = :listingId")
    void deleteAllByListingId(@Param("listingId") Long listingId);
}
