package com.realscore.parsing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.listing.RecordedFetch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Drives the listener through {@link ListingSnapshotService#recordFetch}, the call every scrape run makes per
 * fetched listing. Each test uses its own source to stay isolated in the shared Postgres container.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class SnapshotRecordedListenerTest {

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingDetailRepository listingDetailRepository;

    @Test
    void aNewListingGetsItsDetailRow() throws Exception {
        String source = "listener-created";

        listingSnapshotService.recordFetch(source, "1", fixture("detail_success.json"));

        assertThat(detail(source, "1")).hasValueSatisfying(detail -> assertThat(detail.getOwnership()).isEqualTo("OV"));
    }

    @Test
    void aChangedListingGetsItsDetailRowUpdated() throws Exception {
        String source = "listener-changed";
        listingSnapshotService.recordFetch(source, "1", fixture("detail_success.json"));

        listingSnapshotService.recordFetch(source, "1", fixture("detail_price_on_request.json"));

        assertThat(detail(source, "1")).hasValueSatisfying(detail -> assertThat(detail.getPriceCzk()).isNull());
    }

    @Test
    void aSnapshotThatCannotBeParsedIsStillRecorded() {
        String source = "listener-failure";
        String unstorable = """
                {"result": {"building_type": {"name": "%s", "value": 1}}}
                """.formatted("x".repeat(60));

        RecordedFetch result = listingSnapshotService.recordFetch(source, "1", unstorable);

        assertThat(result).isEqualTo(RecordedFetch.CREATED);
        assertThat(listingSnapshotService.findLatestSnapshot(source, "1")).isPresent();
        assertThat(detail(source, "1")).isEmpty();
    }

    private Optional<ListingDetail> detail(String source, String externalId) {
        Long listingId = listingSnapshotService.findLatestSnapshot(source, externalId).orElseThrow().listingId();
        return listingDetailRepository.findByListingId(listingId);
    }

    private static String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/sreality", name));
    }
}
