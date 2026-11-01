package com.realscore.scraping.internal;

import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.RecordedFetch;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fetches one Sreality estate and records it as a listing snapshot.
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SrealityEstateFetcher {

    static final String SOURCE = "sreality";

    private final SrealityClient client;
    private final ListingSnapshotService listingSnapshotService;

    /**
     * @return what was recorded, or empty if Sreality answered 403/404 and the estate was skipped
     * @throws SrealityApiException if Sreality fails in any other way; nothing is recorded then
     */
    public Optional<RecordedFetch> fetch(long estateId) {
        return client.fetchEstateDetail(estateId)
                .map(body -> listingSnapshotService.recordFetch(SOURCE, String.valueOf(estateId), body));
    }
}
