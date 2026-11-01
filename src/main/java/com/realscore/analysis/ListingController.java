package com.realscore.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** One listing with its price history: {@code GET /api/listings/sreality/2494705740}; 404 if unknown. */
@RestController
@RequiredArgsConstructor
class ListingController {

    private final ListingOverviewService listingOverviewService;

    @GetMapping("/api/listings/{source}/{externalId}")
    ListingResponse listing(@PathVariable String source, @PathVariable String externalId) {
        return listingOverviewService.find(source, externalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No listing " + source + "/" + externalId));
    }
}
