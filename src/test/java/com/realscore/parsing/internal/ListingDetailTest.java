package com.realscore.parsing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ListingDetailTest {

    private static final Instant FIRST = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-11-02T10:00:00Z");

    private static final ListingDetailsFields FIELDS = new ListingDetailsFields(
            new BigDecimal("3590000"), new BigDecimal("37"), 3, "1+1", 1975, null, 1, "C", "Cihlová", true,
            "OV", "Plzeň", "Plzeň-město", "Plzeňský kraj", "Plzeň 2-Slovany", 1, 1);

    @Test
    void firstApplyOnANewRowIsAChange() {
        ListingDetail detail = new ListingDetail(1L);

        assertThat(detail.applyFields(FIELDS, FIRST)).isTrue();
        assertThat(detail.getParsedAt()).isEqualTo(FIRST);
    }

    @Test
    void reapplyingTheSameFieldsChangesNothingIncludingParsedAt() {
        ListingDetail detail = new ListingDetail(1L);
        detail.applyFields(FIELDS, FIRST);

        assertThat(detail.applyFields(FIELDS, SECOND)).isFalse();
        assertThat(detail.getParsedAt()).isEqualTo(FIRST);
    }

    @Test
    void decimalsDifferingOnlyInScaleAreNotAChange() {
        ListingDetail detail = new ListingDetail(1L);
        detail.applyFields(FIELDS, FIRST);

        ListingDetailsFields rescaled = FIELDS.withPriceCzk(new BigDecimal("3590000.00")).withSizeM2(new BigDecimal("37.0"));

        assertThat(detail.applyFields(rescaled, SECOND)).isFalse();
    }

    @Test
    void aChangedFieldOverwritesTheRowAndMovesParsedAt() {
        ListingDetail detail = new ListingDetail(1L);
        detail.applyFields(FIELDS, FIRST);

        assertThat(detail.applyFields(FIELDS.withOwnership("DV"), SECOND)).isTrue();
        assertThat(detail.getOwnership()).isEqualTo("DV");
        assertThat(detail.getParsedAt()).isEqualTo(SECOND);
    }

    @Test
    void aChangedTransactionTypeOrCityPartIsAChange() {
        ListingDetail detail = new ListingDetail(1L);
        detail.applyFields(FIELDS, FIRST);

        assertThat(detail.applyFields(FIELDS.withTransactionTypeCode(2), SECOND)).isTrue();
        assertThat(detail.getTransactionTypeCode()).isEqualTo(2);
        assertThat(detail.applyFields(FIELDS.withTransactionTypeCode(2).withCitypart("Plzeň 3"), SECOND)).isTrue();
        assertThat(detail.getCitypart()).isEqualTo("Plzeň 3");
    }

    @Test
    void aChangedReconstructionYearIsAChange() {
        ListingDetail detail = new ListingDetail(1L);
        detail.applyFields(FIELDS, FIRST);

        assertThat(detail.applyFields(FIELDS.withReconstructionYear(2010), SECOND)).isTrue();
        assertThat(detail.getAcceptanceYear()).isEqualTo(1975);
        assertThat(detail.getReconstructionYear()).isEqualTo(2010);
    }

    @Test
    void aFieldBecomingNullIsAChange() {
        ListingDetail detail = new ListingDetail(1L);
        detail.applyFields(FIELDS, FIRST);

        assertThat(detail.applyFields(FIELDS.withPriceCzk(null), SECOND)).isTrue();
        assertThat(detail.getPriceCzk()).isNull();
    }
}
