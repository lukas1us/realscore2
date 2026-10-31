package com.realscore.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.internal.Listing;
import com.realscore.listing.internal.ListingRepository;
import com.realscore.listing.internal.ListingSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class ListingSnapshotServiceTest {

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingSnapshotRepository listingSnapshotRepository;

    @Test
    void savingTheSamePayloadTwiceWritesExactlyOneSnapshot() {
        listingSnapshotService.recordFetch("sreality", "1", "{\"price\":100}");
        listingSnapshotService.recordFetch("sreality", "1", "{\"price\":100}");

        Listing listing = listingRepository.findBySourceAndExternalId("sreality", "1").orElseThrow();
        assertThat(listingSnapshotRepository.countByListing(listing)).isEqualTo(1);
    }

    @Test
    void savingAChangedPayloadWritesASecondSnapshot() {
        listingSnapshotService.recordFetch("sreality", "2", "{\"price\":100}");
        listingSnapshotService.recordFetch("sreality", "2", "{\"price\":200}");

        Listing listing = listingRepository.findBySourceAndExternalId("sreality", "2").orElseThrow();
        assertThat(listingSnapshotRepository.countByListing(listing)).isEqualTo(2);
    }
}
