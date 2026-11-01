package com.realscore.analysis;

import com.realscore.parsing.ListingDetailsQueryService;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Active listings whose price dropped within the last days, as defined by {@link PriceDrop}. */
@Service
@RequiredArgsConstructor
public class PriceDropService {

    private static final Comparator<PriceDrop> LARGEST_THEN_LATEST = Comparator
            .comparing(PriceDrop::dropPercent).reversed()
            .thenComparing(PriceDrop::droppedAt, Comparator.reverseOrder());

    private final ListingDetailsQueryService listingDetails;

    /**
     * @param days length of the window ending now
     * @param type sale or rent, or {@code null} for both
     * @return the drops, largest first and, for equal drops, latest first
     */
    public List<PriceDrop> find(int days, TransactionType type) {
        Instant since = Instant.now().minus(Duration.ofDays(days));
        return listingDetails.findActivePriceHistoriesChangedSince(since, type == null ? null : type.code()).stream()
                .map(listing -> PriceDrop.of(listing, since))
                .flatMap(Optional::stream)
                .sorted(LARGEST_THEN_LATEST)
                .toList();
    }
}
