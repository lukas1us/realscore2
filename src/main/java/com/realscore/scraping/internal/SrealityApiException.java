package com.realscore.scraping.internal;

/**
 * Sreality answered a request with something other than HTTP 200.
 */
public class SrealityApiException extends RuntimeException {

    private final long estateId;
    private final int statusCode;

    public SrealityApiException(long estateId, int statusCode) {
        super("Sreality returned HTTP " + statusCode + " for estate " + estateId);
        this.estateId = estateId;
        this.statusCode = statusCode;
    }

    public long estateId() {
        return estateId;
    }

    public int statusCode() {
        return statusCode;
    }
}
