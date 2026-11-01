package com.realscore.parsing;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers a re-parse of stored snapshots: {@code POST /api/parsing/reparse?source=sreality} for every listing, or
 * with one or more {@code externalId} parameters for just those. Runs synchronously and returns the counts.
 */
@RestController
@RequiredArgsConstructor
class ReparseController {

    private final ListingDetailsReparseService reparseService;

    @PostMapping("/api/parsing/reparse")
    ReparseResult reparse(@RequestParam String source, @RequestParam(required = false) List<String> externalId) {
        if (externalId == null || externalId.isEmpty()) {
            return reparseService.reparseAll(source);
        }
        return reparseService.reparse(source, externalId);
    }
}
