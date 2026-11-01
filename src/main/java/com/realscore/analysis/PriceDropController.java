package com.realscore.analysis;

import com.realscore.analysis.internal.AnalysisProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Active listings whose price dropped recently: {@code GET /api/analysis/price-drops?days=30&type=sale}. Both
 * parameters are optional: {@code days} defaults to {@code realscore.analysis.price-drop-days}, and without
 * {@code type} sales and rentals are both listed.
 */
@RestController
@RequiredArgsConstructor
class PriceDropController {

    private final PriceDropService priceDropService;
    private final AnalysisProperties properties;

    @GetMapping("/api/analysis/price-drops")
    List<PriceDrop> priceDrops(@RequestParam(required = false) Integer days,
            @RequestParam(required = false) String type) {
        if (days != null && days < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be at least 1");
        }
        return priceDropService.find(days != null ? days : properties.priceDropDays(), transactionType(type));
    }

    private static TransactionType transactionType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "sale" -> TransactionType.SALE;
            case "rent" -> TransactionType.RENT;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be sale or rent");
        };
    }
}
