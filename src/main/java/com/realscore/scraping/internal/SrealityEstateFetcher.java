package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
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
     * @throws SrealityApiException if Sreality does not return the estate; nothing is recorded then
     */
    public void fetch(long estateId) {
        String payload = client.fetchEstateDetail(estateId);
        listingSnapshotService.recordFetch(SOURCE, String.valueOf(estateId), payload);
    }
}
