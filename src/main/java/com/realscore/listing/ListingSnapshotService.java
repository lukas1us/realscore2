package com.realscore.listing;

import com.realscore.listing.internal.Listing;
import com.realscore.listing.internal.ListingRepository;
import com.realscore.listing.internal.ListingSnapshot;
import com.realscore.listing.internal.ListingSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingSnapshotService {

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
