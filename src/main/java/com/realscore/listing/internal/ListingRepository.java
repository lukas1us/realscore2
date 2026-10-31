package com.realscore.listing.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findBySourceAndExternalId(String source, String externalId);

    @Query("select l.externalId from Listing l where l.source = :source and l.externalId in :externalIds")
    List<String> findExternalIdsBySourceAndExternalIdIn(String source, Collection<String> externalIds);
}
