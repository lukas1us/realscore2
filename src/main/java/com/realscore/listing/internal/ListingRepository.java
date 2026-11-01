package com.realscore.listing.internal;

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
    @Query("update Listing l set l.lastSeenAt = :seenAt where l.source = :source and l.externalId in :externalIds")
    int markSeen(String source, Collection<String> externalIds, Instant seenAt);

    @Query("""
            select l.externalId from Listing l
            where l.source = :source and l.removedAt is null and l.lastSeenAt < :notSeenSince
            order by l.id""")
    List<String> findActiveExternalIdsNotSeenSince(String source, Instant notSeenSince);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Listing l set l.removedAt = :removedAt
            where l.source = :source and l.externalId in :externalIds and l.removedAt is null""")
    int markRemoved(String source, Collection<String> externalIds, Instant removedAt);
}
