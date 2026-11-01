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
 * Scored flats for sale, best first: {@code GET /api/analysis/scores?limit=20&city=Plzeň&disposition=2+kk}. All
 * parameters are optional; {@code limit} defaults to {@code realscore.analysis.scores-limit}, and the filters match
 * exactly, ignoring case.
 */
@RestController
@RequiredArgsConstructor
class ListingScoreController {

    private final ListingScoreService listingScoreService;
    private final AnalysisProperties properties;

    @GetMapping("/api/analysis/scores")
    List<ListingScore> scores(@RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String disposition) {
        if (limit != null && limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be at least 1");
        }
        return listingScoreService.top(limit != null ? limit : properties.scoresLimit(), region, city, disposition);
    }
}
