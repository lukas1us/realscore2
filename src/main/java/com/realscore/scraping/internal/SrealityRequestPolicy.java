package com.realscore.scraping.internal;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * The one pacing and retry policy for every Sreality request (see Backlog doc "Sreality API").
 * <ul>
 *     <li>Every attempt, retries included, waits until {@code requestDelay} has passed since the previous one.</li>
 *     <li>HTTP 429, HTTP 5xx and connection errors are retried with exponential backoff, starting at
 *     {@code initialBackoff} and doubling, up to {@code maxAttempts} attempts in total. After the last attempt the
 *     response is returned, or the exception rethrown, as is.</li>
 *     <li>Every other response, including 403 and 404, is returned at once.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
class SrealityRequestPolicy implements ClientHttpRequestInterceptor {

    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final SrealityProperties properties;
    private final Sleeper sleeper;
    private final Clock clock;

    private final ReentrantLock paceLock = new ReentrantLock();
    private Instant lastRequestAt;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        BackOffExecution backOff =
                new ExponentialBackOff(properties.initialBackoff().toMillis(), BACKOFF_MULTIPLIER).start();
        for (int attempt = 1; ; attempt++) {
            boolean lastAttempt = attempt >= properties.maxAttempts();
            pace();
            ClientHttpResponse response;
            try {
                response = execution.execute(request, body);
            } catch (IOException e) {
                if (lastAttempt) {
                    throw e;
                }
                log.warn("Sreality request {} failed ({}), attempt {} of {}; retrying",
                        request.getURI(), e.toString(), attempt, properties.maxAttempts());
                sleep(Duration.ofMillis(backOff.nextBackOff()));
                continue;
            }
            int status = response.getStatusCode().value();
            if (lastAttempt || !isRetriable(status)) {
                return response;
            }
            response.close();
            log.warn("Sreality request {} returned HTTP {}, attempt {} of {}; retrying",
                    request.getURI(), status, attempt, properties.maxAttempts());
            sleep(Duration.ofMillis(backOff.nextBackOff()));
        }
    }

    private static boolean isRetriable(int status) {
        return status == 429 || (status >= 500 && status <= 599);
    }

    private void pace() throws InterruptedIOException {
        paceLock.lock();
        try {
            if (lastRequestAt != null) {
                Duration remaining = Duration.between(clock.instant(), lastRequestAt.plus(properties.requestDelay()));
                if (remaining.isPositive()) {
                    sleep(remaining);
                }
            }
            lastRequestAt = clock.instant();
        } finally {
            paceLock.unlock();
        }
    }

    private void sleep(Duration duration) throws InterruptedIOException {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting between Sreality requests");
        }
    }
}
