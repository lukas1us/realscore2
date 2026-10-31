package com.realscore.parsing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listing_detail")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "price_czk")
    private BigDecimal priceCzk;

    @Column(name = "size_m2")
    private BigDecimal sizeM2;

    @Column(name = "disposition_code")
    private Integer dispositionCode;

    @Column(name = "disposition_label")
    private String dispositionLabel;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "energy_class")
    private String energyClass;

    @Column(name = "construction_type")
    private String constructionType;

    @Column(name = "elevator")
    private Boolean elevator;

    @Column(name = "ownership")
    private String ownership;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "region")
    private String region;

    @Column(name = "parsed_at", nullable = false)
    private Instant parsedAt;

    public ListingDetail(Long listingId) {
        this.listingId = listingId;
    }

    public void applyFields(ListingDetailsFields fields, Instant parsedAt) {
        this.priceCzk = fields.priceCzk();
        this.sizeM2 = fields.sizeM2();
        this.dispositionCode = fields.dispositionCode();
        this.dispositionLabel = fields.dispositionLabel();
        this.yearBuilt = fields.yearBuilt();
        this.floor = fields.floor();
        this.energyClass = fields.energyClass();
        this.constructionType = fields.constructionType();
        this.elevator = fields.elevator();
        this.ownership = fields.ownership();
        this.city = fields.city();
        this.district = fields.district();
        this.region = fields.region();
        this.parsedAt = parsedAt;
    }
}
