package com.realscore.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realscore.PostgresContainerConfig;
import com.realscore.listing.ListingSnapshotService;
import com.realscore.parsing.internal.ListingDetail;
import com.realscore.parsing.internal.ListingDetailRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

/**
 * {@code recordFetch} already parses each new snapshot (TASK-33), so tests set up what a re-parse is for: listings
 * stored before automatic parsing ({@link #recordWithoutDetail}) or rows derived by an older parser
 * ({@link #makeStale}). Each test uses its own source so the shared Postgres container's listings from other tests
 * don't leak in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfig.class)
class ListingDetailsReparseServiceTest {

    /** Valid JSON whose construction type is longer than listing_detail.construction_type allows. */
    private static final String UNSTORABLE_PAYLOAD = """
            {"result": {"building_type": {"name": "%s", "value": 1}}}
            """.formatted("x".repeat(60));

    @LocalServerPort
    int port;

    @Autowired
    private ListingSnapshotService listingSnapshotService;

    @Autowired
    private ListingDetailsReparseService reparseService;

    @Autowired
    private ListingDetailRepository listingDetailRepository;

    @Test
    void reparsesEveryStoredListingOfTheSource() throws Exception {
        String source = "reparse-all";
        recordWithoutDetail(source, "1", fixture("detail_success.json"));
        recordWithoutDetail(source, "2", fixture("detail_price_on_request.json"));

        ReparseResult result = reparseService.reparseAll(source);

        assertThat(result).isEqualTo(new ReparseResult(2, 0, 0, 0));
        assertThat(detail(source, "1").orElseThrow().getOwnership()).isEqualTo("OV");
        assertThat(detail(source, "2").orElseThrow().getPriceCzk()).isNull();
    }

    @Test
    void secondRunWithoutParserChangesLeavesRowsIdentical() throws Exception {
        String source = "reparse-twice";
        recordWithoutDetail(source, "1", fixture("detail_success.json"));
        recordWithoutDetail(source, "2", fixture("detail_price_on_request.json"));
        reparseService.reparseAll(source);
        List<ListingDetail> afterFirstRun = List.of(detail(source, "1").orElseThrow(), detail(source, "2").orElseThrow());

        ReparseResult second = reparseService.reparseAll(source);

        assertThat(second).isEqualTo(new ReparseResult(0, 0, 2, 0));
        List<ListingDetail> afterSecondRun = List.of(detail(source, "1").orElseThrow(), detail(source, "2").orElseThrow());
        assertThat(afterSecondRun).usingRecursiveFieldByFieldElementComparator().isEqualTo(afterFirstRun);
    }

    @Test
    void reparsesOnlyTheGivenSubset() throws Exception {
        String source = "reparse-subset";
        recordWithoutDetail(source, "1", fixture("detail_success.json"));
        recordWithoutDetail(source, "2", fixture("detail_price_on_request.json"));

        ReparseResult result = reparseService.reparse(source, List.of("2"));

        assertThat(result).isEqualTo(new ReparseResult(1, 0, 0, 0));
        assertThat(detail(source, "1")).isEmpty();
        assertThat(detail(source, "2")).isPresent();
    }

    @Test
    void aRowFromAnOlderParserIsOverwritten() throws Exception {
        String source = "reparse-update";
        listingSnapshotService.recordFetch(source, "1", fixture("detail_success.json"));
        makeStale(source, "1");

        ReparseResult result = reparseService.reparseAll(source);

        assertThat(result).isEqualTo(new ReparseResult(0, 1, 0, 0));
        assertThat(detail(source, "1").orElseThrow().getOwnership()).isEqualTo("OV");
    }

    @Test
    void reparseFillsColumnsAddedAfterARowWasParsed() throws Exception {
        String source = "reparse-new-columns";
        listingSnapshotService.recordFetch(source, "1", fixture("detail_success.json"));
        ListingDetail beforeV6 = detail(source, "1").orElseThrow();
        beforeV6.setTransactionTypeCode(null);
        beforeV6.setPropertyCategoryCode(null);
        beforeV6.setCitypart(null);
        listingDetailRepository.save(beforeV6);

        ReparseResult result = reparseService.reparseAll(source);

        assertThat(result).isEqualTo(new ReparseResult(0, 1, 0, 0));
        ListingDetail reparsed = detail(source, "1").orElseThrow();
        assertThat(reparsed.getTransactionTypeCode()).isEqualTo(1);
        assertThat(reparsed.getPropertyCategoryCode()).isEqualTo(1);
        assertThat(reparsed.getCitypart()).isEqualTo("Plzeň 2-Slovany");
    }

    @Test
    void aFailingListingIsCountedAndDoesNotStopTheOthers() throws Exception {
        String source = "reparse-failure";
        recordWithoutDetail(source, "1", UNSTORABLE_PAYLOAD);
        recordWithoutDetail(source, "2", fixture("detail_success.json"));

        ReparseResult result = reparseService.reparse(source, List.of("1", "2", "unknown"));

        assertThat(result).isEqualTo(new ReparseResult(1, 0, 0, 2));
        assertThat(detail(source, "1")).isEmpty();
        assertThat(detail(source, "2")).isPresent();
    }

    @Test
    void endpointReparsesAllOrTheGivenIds() throws Exception {
        String source = "reparse-endpoint";
        recordWithoutDetail(source, "1", fixture("detail_success.json"));
        recordWithoutDetail(source, "2", fixture("detail_price_on_request.json"));
        RestClient client = RestClient.create("http://localhost:" + port);

        ReparseResult subset = client.post()
                .uri("/api/parsing/reparse?source={source}&externalId=1", source)
                .retrieve()
                .body(ReparseResult.class);
        ReparseResult all = client.post()
                .uri("/api/parsing/reparse?source={source}", source)
                .retrieve()
                .body(ReparseResult.class);

        assertThat(subset).isEqualTo(new ReparseResult(1, 0, 0, 0));
        assertThat(all).isEqualTo(new ReparseResult(1, 0, 1, 0));
    }

    /** Records a snapshot and drops the detail row automatic parsing wrote for it. */
    private void recordWithoutDetail(String source, String externalId, String payload) {
        listingSnapshotService.recordFetch(source, externalId, payload);
        detail(source, externalId).ifPresent(listingDetailRepository::delete);
    }

    /** Overwrites a parsed field, as if an older parser had derived it differently. */
    private void makeStale(String source, String externalId) {
        ListingDetail detail = detail(source, externalId).orElseThrow();
        detail.setOwnership("STALE");
        listingDetailRepository.save(detail);
    }

    private Optional<ListingDetail> detail(String source, String externalId) {
        Long listingId = listingSnapshotService.findLatestSnapshot(source, externalId).orElseThrow().listingId();
        return listingDetailRepository.findByListingId(listingId);
    }

    private static String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/sreality", name));
    }
}
