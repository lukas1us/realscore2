package com.realscore.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.parsing.internal.ListingDetail;
import com.realscore.parsing.internal.ListingDetailRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class ListingDetailsParsingServiceTest {

    private static final String SOURCE = "sreality";

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingDetailsParsingService listingDetailsParsingService;

    @Autowired
    private ListingDetailRepository listingDetailRepository;

    @Test
    void parsingTheSameSnapshotTwiceOverwritesRatherThanDuplicates() throws Exception {
        String externalId = "2494705740";
        String payload = Files.readString(Path.of("src/test/resources/fixtures/sreality/detail_success.json"));
        listingSnapshotService.recordFetch(SOURCE, externalId, payload);

        listingDetailsParsingService.parse(SOURCE, externalId);
        listingDetailsParsingService.parse(SOURCE, externalId);

        Long listingId = listingSnapshotService.findLatestSnapshot(SOURCE, externalId).orElseThrow().listingId();
        assertThat(listingDetailRepository.findByListingId(listingId)).isPresent();

        ListingDetail detail = listingDetailRepository.findByListingId(listingId).orElseThrow();
        assertThat(detail.getPriceCzk()).isEqualByComparingTo(new BigDecimal("3590000"));
        assertThat(detail.getOwnership()).isEqualTo("OV");
    }
}
