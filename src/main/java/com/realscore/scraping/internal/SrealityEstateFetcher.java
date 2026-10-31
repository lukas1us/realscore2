package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Fetches one Sreality estate and records it as a listing snapshot.
 */
@Component
public class SrealityEstateFetcher {

    static final String SOURCE = "sreality";

    private final SrealityClient client;
    private final ListingSnapshotService listingSnapshotService;

    SrealityEstateFetcher(SrealityClient client, ListingSnapshotService listingSnapshotService) {
        this.client = client;
        this.listingSnapshotService = listingSnapshotService;
    }

    /**
     * @return {@code true} if the estate was recorded, {@code false} if Sreality answered 403/404 and it was skipped
     * @throws SrealityApiException if Sreality fails in any other way; nothing is recorded then
     */
    public boolean fetch(long estateId) {
        Optional<String> payload = client.fetchEstateDetail(estateId);
        payload.ifPresent(body -> listingSnapshotService.recordFetch(SOURCE, String.valueOf(estateId), body));
        return payload.isPresent();
    }
}
