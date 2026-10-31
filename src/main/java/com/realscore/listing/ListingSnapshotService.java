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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingSnapshotService {

    /** Keeps the {@code IN (...)} list well under Postgres' bind-parameter limit. */
    private static final int KNOWN_IDS_BATCH_SIZE = 1000;

    private final ListingRepository listingRepository;
    private final ListingSnapshotRepository snapshotRepository;

    @Transactional
    public void recordFetch(String source, String externalId, String payload) {
        Instant now = Instant.now();
        Listing listing = listingRepository.findBySourceAndExternalId(source, externalId)
                .map(existing -> {
                    existing.setLastSeenAt(now);
                    return existing;
                })
                .orElseGet(() -> new Listing(source, externalId, now));
        listing = listingRepository.save(listing);

        String payloadHash = sha256Hex(payload);
        boolean unchanged = snapshotRepository.findTopByListingOrderByFetchedAtDesc(listing)
                .map(latest -> latest.getPayloadHash().equals(payloadHash))
                .orElse(false);
        if (unchanged) {
            return;
        }
        snapshotRepository.save(new ListingSnapshot(listing, payload, payloadHash, now));
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
        List<String> ids = List.copyOf(externalIds);
        Set<String> known = new HashSet<>();
        for (int from = 0; from < ids.size(); from += KNOWN_IDS_BATCH_SIZE) {
            List<String> batch = ids.subList(from, Math.min(from + KNOWN_IDS_BATCH_SIZE, ids.size()));
            known.addAll(listingRepository.findExternalIdsBySourceAndExternalIdIn(source, batch));
        }
        return known;
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
