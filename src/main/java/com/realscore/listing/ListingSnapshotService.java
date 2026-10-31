package com.realscore.listing;

import com.realscore.listing.internal.Listing;
import com.realscore.listing.internal.ListingRepository;
import com.realscore.listing.internal.ListingSnapshot;
import com.realscore.listing.internal.ListingSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingSnapshotService {

    /** Keeps the {@code IN (...)} list well under Postgres' bind-parameter limit. */
    private static final int ID_BATCH_SIZE = 1000;

    private final ListingRepository listingRepository;
    private final ListingSnapshotRepository snapshotRepository;

    /**
     * Records one fetch of a listing's detail: creates the listing if needed, moves {@code last_seen_at} and
     * {@code last_fetched_at}, and writes a snapshot unless the payload equals the latest one.
     */
    @Transactional
    public RecordedFetch recordFetch(String source, String externalId, String payload) {
        Instant now = Instant.now();
        Optional<Listing> existing = listingRepository.findBySourceAndExternalId(source, externalId);
        Listing listing = existing
                .map(known -> {
                    known.setLastSeenAt(now);
                    known.setLastFetchedAt(now);
                    return known;
                })
                .orElseGet(() -> new Listing(source, externalId, now));
        listing = listingRepository.save(listing);

        String payloadHash = sha256Hex(payload);
        boolean unchanged = snapshotRepository.findTopByListingOrderByFetchedAtDesc(listing)
                .map(latest -> latest.getPayloadHash().equals(payloadHash))
                .orElse(false);
        if (unchanged) {
            return RecordedFetch.UNCHANGED;
        }
        snapshotRepository.save(new ListingSnapshot(listing, payload, payloadHash, now));
        return existing.isPresent() ? RecordedFetch.CHANGED : RecordedFetch.CREATED;
    }

    public Optional<ListingSnapshotView> findLatestSnapshot(String source, String externalId) {
        return listingRepository.findBySourceAndExternalId(source, externalId)
                .flatMap(snapshotRepository::findTopByListingOrderByFetchedAtDesc)
                .map(snapshot -> new ListingSnapshotView(
                        snapshot.getListing().getId(), source, externalId, snapshot.getPayload(), snapshot.getFetchedAt()));
    }

    /**
     * Returns those of the given external IDs that already have a listing from {@code source}.
     */
    @Transactional(readOnly = true)
    public Set<String> findKnownExternalIds(String source, Collection<String> externalIds) {
        Set<String> known = new HashSet<>();
        forEachBatch(externalIds, batch -> known.addAll(
                listingRepository.findExternalIdsBySourceAndExternalIdIn(source, batch)));
        return known;
    }

    /**
     * Returns those of the given external IDs from {@code source} whose detail was last fetched before
     * {@code cutoff}.
     */
    @Transactional(readOnly = true)
    public Set<String> findExternalIdsFetchedBefore(String source, Collection<String> externalIds, Instant cutoff) {
        Set<String> due = new HashSet<>();
        forEachBatch(externalIds, batch -> due.addAll(
                listingRepository.findExternalIdsFetchedBefore(source, batch, cutoff)));
        return due;
    }

    /**
     * Records which listings from {@code source} a run saw: each seen listing gets {@code last_seen_at = now} and, if
     * it was marked removed, has {@code removed_at} cleared; every other active listing not seen since
     * {@code runStartedAt} gets {@code removed_at = now}. Listings first recorded during the run count as seen.
     *
     * @param seenExternalIds every ID the run found, already known or not
     */
    @Transactional
    public PresenceResult recordPresence(String source, Collection<String> seenExternalIds, Instant runStartedAt) {
        Instant now = Instant.now();
        int[] reappeared = {0};
        forEachBatch(seenExternalIds, batch -> {
            reappeared[0] += listingRepository.clearRemovedAt(source, batch);
            listingRepository.markSeen(source, batch, now);
        });
        int removed = listingRepository.markRemovedIfNotSeenSince(source, runStartedAt, now);
        return new PresenceResult(reappeared[0], removed);
    }

    private static void forEachBatch(Collection<String> externalIds, Consumer<List<String>> action) {
        List<String> ids = List.copyOf(externalIds);
        for (int from = 0; from < ids.size(); from += ID_BATCH_SIZE) {
            action.accept(ids.subList(from, Math.min(from + ID_BATCH_SIZE, ids.size())));
        }
    }

    private static String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
