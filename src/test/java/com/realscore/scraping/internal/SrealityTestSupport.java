package com.realscore.scraping.internal;

import java.time.Duration;

/**
 * Builds Sreality settings and clients for tests that talk to WireMock instead of Sreality.
 */
final class SrealityTestSupport {

    static final String SEARCH_PATH = "/api/v1/estates/search";
    static final int MAX_ATTEMPTS = 3;
    static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
    static final Duration REQUEST_DELAY = Duration.ofMillis(500);

    private SrealityTestSupport() {
    }

    static SrealityProperties properties(String baseUrl, int bucketThreshold, int pageSize) {
        return new SrealityProperties(baseUrl, "test-agent", "https://www.sreality.cz/", "/api/v1/estates/{id}",
                SEARCH_PATH, bucketThreshold, pageSize, MAX_ATTEMPTS, INITIAL_BACKOFF, REQUEST_DELAY);
    }

    /** A client whose waits only move {@code time} forward. */
    static SrealityClient client(SrealityProperties properties, FakeTime time) {
        return new SrealityClient(properties, new SrealityRequestPolicy(properties, time, time));
    }
}
