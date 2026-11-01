package com.realscore.analysis;

import com.realscore.parsing.ListingDetailsQueryService;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** One listing with its parsed details, price history and the figures derived from them. */
@Service
@RequiredArgsConstructor
public class ListingOverviewService {

    private final ListingDetailsQueryService listingDetails;

    /** @return the listing, removed or not, or empty if no listing from {@code source} has that external ID */
    public Optional<ListingResponse> find(String source, String externalId) {
        Instant now = Instant.now();
        return listingDetails.findListing(source, externalId)
                .map(listing -> ListingResponse.of(listing, ListingStats.of(listing, now)));
    }
}
