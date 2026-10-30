package com.realscore.service;

import com.realscore.model.Property;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Year;

@Service
public class PropertyScoringService {

    private static final double BASELINE_PRICE_PER_SQFT = 150.0;
    private static final double PRICE_PER_SQFT_PENALTY = 5.0;
    private static final double AGE_PENALTY_PER_YEAR = 1.0;
    private static final double BED_BATH_WEIGHT = 10.0;

    private final Clock clock;

    public PropertyScoringService() {
        this(Clock.systemDefaultZone());
    }

    public PropertyScoringService(Clock clock) {
        this.clock = clock;
    }

    public double score(Property property) {
        double valueScore = valueScore(property);
        double ageScore = ageScore(property);
        double bedBathScore = bedBathScore(property);

        return 0.5 * valueScore + 0.3 * ageScore + 0.2 * bedBathScore;
    }

    private double valueScore(Property property) {
        double pricePerSqFt = property.getPrice() / property.getSquareFootage();
        double score = 100 - (pricePerSqFt - BASELINE_PRICE_PER_SQFT) / PRICE_PER_SQFT_PENALTY;
        return clamp(score);
    }

    private double ageScore(Property property) {
        int currentYear = Year.now(clock).getValue();
        int age = Math.max(0, currentYear - property.getYearBuilt());
        return clamp(100 - age * AGE_PENALTY_PER_YEAR);
    }

    private double bedBathScore(Property property) {
        return clamp((property.getBedrooms() + property.getBathrooms()) * BED_BATH_WEIGHT);
    }

    private double clamp(double score) {
        return Math.max(0, Math.min(100, score));
    }
}
