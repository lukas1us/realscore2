package com.realscore.listing.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findBySourceAndExternalId(String source, String externalId);

    @Query("select l.externalId from Listing l where l.source = :source order by l.id")
    List<String> findExternalIdsBySource(String source);

    @Query("select l.externalId from Listing l where l.source = :source and l.externalId in :externalIds")
    List<String> findExternalIdsBySourceAndExternalIdIn(String source, Collection<String> externalIds);

    @Query("""
            select l.externalId from Listing l
            where l.source = :source and l.externalId in :externalIds and l.lastFetchedAt < :cutoff""")
    List<String> findExternalIdsFetchedBefore(String source, Collection<String> externalIds, Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Listing l set l.removedAt = null
            where l.source = :source and l.externalId in :externalIds and l.removedAt is not null""")
    int clearRemovedAt(String source, Collection<String> externalIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Listing l set l.outOfScopeSince = null
            where l.source = :source and l.externalId in :externalIds and l.outOfScopeSince is not null""")
    int clearOutOfScope(String source, Collection<String> externalIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Listing l set l.outOfScopeSince = :since
            where l.source = :source and l.externalId in :externalIds and l.removedAt is null
              and l.outOfScopeSince is null""")
    int markOutOfScope(String source, Collection<String> externalIds, Instant since);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Listing l set l.lastSeenAt = :seenAt where l.source = :source and l.externalId in :externalIds")
    int markSeen(String source, Collection<String> externalIds, Instant seenAt);

    /** @return pairs of external ID and search price, for listings that have a search price */
    @Query("""
            select l.externalId, l.searchPriceCzk from Listing l
            where l.source = :source and l.externalId in :externalIds and l.searchPriceCzk is not null""")
    List<Object[]> findSearchPrices(String source, Collection<String> externalIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Listing l set l.searchPriceCzk = :searchPrice
            where l.source = :source and l.externalId = :externalId""")
    int updateSearchPrice(String source, String externalId, BigDecimal searchPrice);

    @Query("""
            select l.externalId from Listing l
            where l.source = :source and l.removedAt is null and l.lastSeenAt < :notSeenSince
              and (l.outOfScopeSince is null or l.lastFetchedAt < :outOfScopeFetchedBefore)
            order by l.id""")
    List<String> findActiveExternalIdsNotSeenSince(String source, Instant notSeenSince,
            Instant outOfScopeFetchedBefore);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Listing l set l.removedAt = :removedAt, l.outOfScopeSince = null
            where l.source = :source and l.externalId in :externalIds and l.removedAt is null""")
    int markRemoved(String source, Collection<String> externalIds, Instant removedAt);
}
