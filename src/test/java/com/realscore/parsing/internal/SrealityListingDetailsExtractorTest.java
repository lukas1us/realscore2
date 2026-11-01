package com.realscore.parsing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.ObjectMapper;

class SrealityListingDetailsExtractorTest {

    private final SrealityListingDetailsExtractor extractor = new SrealityListingDetailsExtractor(new ObjectMapper());

    @Test
    void extractsEveryFieldFromAFullyPopulatedListing() throws Exception {
        ListingDetailsFields fields = extractor.extract(fixture("detail_success.json"));

        assertThat(fields.priceCzk()).isEqualByComparingTo(new BigDecimal("3590000"));
        assertThat(fields.sizeM2()).isEqualByComparingTo(new BigDecimal("37"));
        assertThat(fields.dispositionCode()).isEqualTo(3);
        assertThat(fields.dispositionLabel()).isEqualTo("1+1");
        assertThat(fields.yearBuilt()).isEqualTo(1975);
        assertThat(fields.floor()).isEqualTo(1);
        assertThat(fields.energyClass()).isEqualTo("C");
        assertThat(fields.constructionType()).isEqualTo("Cihlová");
        assertThat(fields.elevator()).isTrue();
        assertThat(fields.ownership()).isEqualTo("OV");
        assertThat(fields.city()).isEqualTo("Plzeň");
        assertThat(fields.district()).isEqualTo("Plzeň-město");
        assertThat(fields.region()).isEqualTo("Plzeňský kraj");
        assertThat(fields.citypart()).isEqualTo("Plzeň 2-Slovany");
        assertThat(fields.transactionTypeCode()).isEqualTo(1);
        assertThat(fields.propertyCategoryCode()).isEqualTo(1);
    }

    @Test
    void extractsTransactionTypeAndCategoryOfARentalCommercialListing() throws Exception {
        ListingDetailsFields fields = extractor.extract(fixture("detail_price_on_request.json"));

        assertThat(fields.transactionTypeCode()).isEqualTo(2);
        assertThat(fields.propertyCategoryCode()).isEqualTo(4);
        assertThat(fields.citypart()).isEqualTo("Staré Město");
    }

    @Test
    void missingTransactionTypeCategoryAndCityPartYieldNull() {
        ListingDetailsFields fields = extractor.extract("{\"result\": {\"locality\": {\"city\": \"Plzeň\"}}}");

        assertThat(fields.transactionTypeCode()).isNull();
        assertThat(fields.propertyCategoryCode()).isNull();
        assertThat(fields.citypart()).isNull();
    }

    @Test
    void priceOnRequestYieldsNoPriceWithoutError() throws Exception {
        ListingDetailsFields fields = extractor.extract(fixture("detail_price_on_request.json"));

        assertThat(fields.priceCzk()).isNull();
        assertThat(fields.sizeM2()).isEqualByComparingTo(new BigDecimal("137"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, OV",
            "2, DV",
            "3, OTHER",
            "99, OTHER",
    })
    void mapsOwnershipCodes(int ownershipValue, String expected) {
        String payload = """
                {"result": {"ownership": {"name": "x", "value": %d}}}
                """.formatted(ownershipValue);

        assertThat(extractor.extract(payload).ownership()).isEqualTo(expected);
    }

    @Test
    void missingOwnershipYieldsNull() {
        ListingDetailsFields fields = extractor.extract("{\"result\": {}}");

        assertThat(fields.ownership()).isNull();
    }

    @Test
    void fallsBackToReconstructionYearWhenAcceptanceYearMissing() {
        String payload = """
                {"result": {"reconstruction_year": 2010}}
                """;

        assertThat(extractor.extract(payload).yearBuilt()).isEqualTo(2010);
    }

    @Test
    void fallsBackToFloorAreaWhenUsableAreaMissing() {
        String payload = """
                {"result": {"floor_area": 50}}
                """;

        assertThat(extractor.extract(payload).sizeM2()).isEqualByComparingTo(new BigDecimal("50"));
    }

    private static String fixture(String name) throws Exception {
        Path path = Path.of("src/test/resources/fixtures/sreality", name);
        return Files.readString(path);
    }
}
