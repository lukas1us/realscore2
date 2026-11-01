package com.realscore.scraping.internal;

/**
 * How Sreality answered a request for one estate's detail.
 */
public sealed interface EstateDetail {

    /** HTTP 200: the response body exactly as received, including the {@code {"result": ...}} wrapper. */
    record Found(String body) implements EstateDetail {
    }

    /** HTTP 404 or 410: the estate no longer exists. */
    record Gone(int statusCode) implements EstateDetail {
    }

    /**
     * HTTP 403: the estate is hidden, or Sreality refuses the request. That does not prove the estate is gone, so it
     * is never a reason to mark a listing removed.
     */
    record Forbidden() implements EstateDetail {
    }
}
