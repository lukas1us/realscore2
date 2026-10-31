package com.realscore.scraping.internal;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Sreality answered a request with something other than HTTP 200.
 */
@Getter
@Accessors(fluent = true)
public class SrealityApiException extends RuntimeException {

    /**
     * The requested estate, or {@code null} if the request was not about a single estate.
     */
    private final Long estateId;
    private final String requestPath;
    private final int statusCode;

    public SrealityApiException(long estateId, String requestPath, int statusCode) {
        this((Long) estateId, requestPath, statusCode);
    }

    /**
     * For requests that are not about a single estate, such as a search.
     */
    public SrealityApiException(String requestPath, int statusCode) {
        this(null, requestPath, statusCode);
    }

    private SrealityApiException(Long estateId, String requestPath, int statusCode) {
        super("Sreality returned HTTP " + statusCode + " for " + requestPath);
        this.estateId = estateId;
        this.requestPath = requestPath;
        this.statusCode = statusCode;
    }
}
