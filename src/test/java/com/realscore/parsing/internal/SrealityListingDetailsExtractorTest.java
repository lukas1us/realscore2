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
        assertThat(fields.acceptanceYear()).isEqualTo(1975);
        assertThat(fields.reconstructionYear()).isNull();
        assertThat(fields.floor()).isEqualTo(1);
        assertThat(fields.energyClass()).isEqualTo("C");
        assertThat(fields.constructionType()).isEqualTo("Cihlová");
        assertThat(fields.elevator()).isTrue();
        assertThat(fields.ownership()).isEqualTo("OV");
        assertThat(fields.buildingConditionCode()).isEqualTo(1);
        assertThat(fields.buildingConditionLabel()).isEqualTo("Velmi dobrý");
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

    @ParameterizedTest
    @CsvSource({
            "1, Ano, true",
            "2, Ne, false",
            "0, - nezadáno, ",
            "99, x, ",
    })
    void mapsElevatorCodesWithNotEnteredAsUnknown(int elevatorValue, String name, Boolean expected) {
        String payload = """
                {"result": {"elevator": {"name": "%s", "value": %d}}}
                """.formatted(name, elevatorValue);

        assertThat(extractor.extract(payload).elevator()).isEqualTo(expected);
    }

    @Test
    void missingElevatorYieldsNull() {
        assertThat(extractor.extract("{\"result\": {}}").elevator()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "0, - vyber třídu, ",
            "1, A - Mimořádně úsporná, A",
            "2, B - Velmi úsporná, B",
            "3, C - Úsporná, C",
            "4, D - Méně úsporná, D",
            "5, E - Nehospodárná, E",
            "6, F - Velmi nehospodárná, F",
            "7, G - Mimořádně nehospodárná, G",
            "99, X - neznámá, ",
    })
    void mapsEnergyClassFromCodeWithNoClassChosenAsUnknown(int ratingValue, String name, String expected) {
        String payload = """
                {"result": {"energy_efficiency_rating_cb": {"name": "%s", "value": %d}}}
                """.formatted(name, ratingValue);

        assertThat(extractor.extract(payload).energyClass()).isEqualTo(expected);
    }

    @Test
    void energyClassIgnoresTheNameText() {
        String payload = """
                {"result": {"energy_efficiency_rating_cb": {"name": "B - Velmi úsporná", "value": 4}}}
                """;

        assertThat(extractor.extract(payload).energyClass()).isEqualTo("D");
    }

    @Test
    void missingEnergyClassYieldsNull() {
        assertThat(extractor.extract("{\"result\": {}}").energyClass()).isNull();
    }

    @Test
    void missingOwnershipYieldsNull() {
        ListingDetailsFields fields = extractor.extract("{\"result\": {}}");

        assertThat(fields.ownership()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "6, Novostavba",
            "8, Před rekonstrukcí",
            "9, Po rekonstrukci",
    })
    void keepsTheBuildingConditionCodeAndLabelAsGiven(int conditionValue, String name) {
        String payload = """
                {"result": {"building_condition": {"name": "%s", "value": %d}}}
                """.formatted(name, conditionValue);

        ListingDetailsFields fields = extractor.extract(payload);

        assertThat(fields.buildingConditionCode()).isEqualTo(conditionValue);
        assertThat(fields.buildingConditionLabel()).isEqualTo(name);
    }

    @Test
    void notEnteredBuildingConditionYieldsNull() {
        String payload = """
                {"result": {"building_condition": {"name": "- nezadáno", "value": 0}}}
                """;

        ListingDetailsFields fields = extractor.extract(payload);

        assertThat(fields.buildingConditionCode()).isNull();
        assertThat(fields.buildingConditionLabel()).isNull();
    }

    @Test
    void missingBuildingConditionYieldsNull() {
        ListingDetailsFields fields = extractor.extract("{\"result\": {}}");

        assertThat(fields.buildingConditionCode()).isNull();
        assertThat(fields.buildingConditionLabel()).isNull();
    }

    @Test
    void aReconstructionYearIsNotTakenAsTheAcceptanceYear() throws Exception {
        ListingDetailsFields fields = extractor.extract(fixture("detail_reconstruction_year_only.json"));

        assertThat(fields.acceptanceYear()).isNull();
        assertThat(fields.reconstructionYear()).isEqualTo(2025);
    }

    @Test
    void readsBothYearsWhenBothAreGiven() {
        String payload = """
                {"result": {"acceptance_year": 1975, "reconstruction_year": 2010}}
                """;

        ListingDetailsFields fields = extractor.extract(payload);

        assertThat(fields.acceptanceYear()).isEqualTo(1975);
        assertThat(fields.reconstructionYear()).isEqualTo(2010);
    }

    @Test
    void fallsBackToFloorAreaWhenUsableAreaMissing() {
        String payload = """
                {"result": {"floor_area": 50}}
                """;

        assertThat(extractor.extract(payload).sizeM2()).isEqualByComparingTo(new BigDecimal("50"));
    }

    @Test
    void buildsTheSaleDetailUrl() throws Exception {
        assertThat(extractor.extract(fixture("detail_success.json")).url())
                .isEqualTo("https://www.sreality.cz/detail/prodej/byt/1+1/plzen-plzen-2-slovany-barakova/2494705740");
    }

    @Test
    void buildsAUrlWithoutStreetOrCitypart() throws Exception {
        assertThat(extractor.extract(fixture("detail_no_street_or_citypart.json")).url())
                .isEqualTo("https://www.sreality.cz/detail/pronajem/byt/1+kk/plzen/2494705740");
    }

    @Test
    void buildsTheRentalUrlConfirmedOnTheSite() {
        String payload = urlPayload(2, 1, "1+kk",
                "\"city_seo_name\": \"plzen\", \"citypart_seo_name\": \"jizni-predmesti\", "
                        + "\"street_seo_name\": \"magisterska\"", "4088148044");

        assertThat(extractor.extract(payload).url())
                .isEqualTo("https://www.sreality.cz/detail/pronajem/byt/1+kk/plzen-jizni-predmesti-magisterska/4088148044");
    }

    @Test
    void aRepeatedLocalityPartAppearsOnce() {
        String payload = urlPayload(1, 1, "2+kk",
                "\"city_seo_name\": \"praha\", \"citypart_seo_name\": \"praha\", \"street_seo_name\": \"vodickova\"",
                "123");

        assertThat(extractor.extract(payload).url())
                .isEqualTo("https://www.sreality.cz/detail/prodej/byt/2+kk/praha-vodickova/123");
    }

    @Test
    void blankLocalityPartsAreSkipped() {
        String payload = urlPayload(1, 1, "2+kk",
                "\"city_seo_name\": \"brno\", \"citypart_seo_name\": \"  \", \"street_seo_name\": \"\"", "123");

        assertThat(extractor.extract(payload).url())
                .isEqualTo("https://www.sreality.cz/detail/prodej/byt/2+kk/brno/123");
    }

    @Test
    void theSubSegmentIsLowercasedWithoutAccents() {
        String payload = urlPayload(1, 1, "Atypický", "\"city_seo_name\": \"brno\"", "123");

        assertThat(extractor.extract(payload).url())
                .isEqualTo("https://www.sreality.cz/detail/prodej/byt/atypicky/brno/123");
    }

    @ParameterizedTest
    @CsvSource({"1, byt", "2, dum", "3, pozemek", "4, komercni", "5, ostatni"})
    void mapsTheMainCategory(int mainValue, String expected) {
        String payload = urlPayload(1, mainValue, "x", "\"city_seo_name\": \"brno\"", "123");

        assertThat(extractor.extract(payload).url()).startsWith("https://www.sreality.cz/detail/prodej/" + expected + "/");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "unknown type | {\"result\": {\"category_type_cb\": {\"value\": 3}, \"category_main_cb\": {\"value\": 1}, \"category_sub_cb\": {\"name\": \"1+1\"}, \"locality\": {\"city_seo_name\": \"brno\"}, \"hash_id\": 1}}",
            "unknown main | {\"result\": {\"category_type_cb\": {\"value\": 1}, \"category_main_cb\": {\"value\": 9}, \"category_sub_cb\": {\"name\": \"1+1\"}, \"locality\": {\"city_seo_name\": \"brno\"}, \"hash_id\": 1}}",
            "blank sub | {\"result\": {\"category_type_cb\": {\"value\": 1}, \"category_main_cb\": {\"value\": 1}, \"category_sub_cb\": {\"name\": \" \"}, \"locality\": {\"city_seo_name\": \"brno\"}, \"hash_id\": 1}}",
            "no locality | {\"result\": {\"category_type_cb\": {\"value\": 1}, \"category_main_cb\": {\"value\": 1}, \"category_sub_cb\": {\"name\": \"1+1\"}, \"locality\": {\"city_seo_name\": \"\"}, \"hash_id\": 1}}",
            "no hash_id | {\"result\": {\"category_type_cb\": {\"value\": 1}, \"category_main_cb\": {\"value\": 1}, \"category_sub_cb\": {\"name\": \"1+1\"}, \"locality\": {\"city_seo_name\": \"brno\"}}}"})
    void noUrlWhenARequiredPartIsMissing(String ignored, String payload) {
        assertThat(extractor.extract(payload).url()).isNull();
    }

    private static String urlPayload(int type, int main, String sub, String locality, String hashId) {
        return """
                {"result": {"category_type_cb": {"value": %d}, "category_main_cb": {"value": %d},
                 "category_sub_cb": {"name": "%s"}, "locality": {%s}, "hash_id": %s}}
                """.formatted(type, main, sub, locality, hashId);
    }

    private static String fixture(String name) throws Exception {
        Path path = Path.of("src/test/resources/fixtures/sreality", name);
        return Files.readString(path);
    }
}
