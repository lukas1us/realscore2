package com.realscore.scraping.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Sreality API settings; values live in {@code application.properties} under {@code realscore.sreality}.
 *
 * @param detailPath estate detail path, with an {@code {id}} placeholder for the estate ID
 * @param bucketThreshold a search bucket with more estates than this is split further (see {@link SrealityIdCollector})
 * @param pageSize how many estates to request per search page
 * @param maxAttempts attempts per request in total, the first included (see {@link SrealityRequestPolicy})
 * @param initialBackoff wait before the first retry; doubles with every further retry
 * @param requestDelay minimum time between the starts of two requests
 */
@Validated
@ConfigurationProperties("realscore.sreality")
public record SrealityProperties(
        @NotBlank String baseUrl,
        @NotBlank String userAgent,
        @NotBlank String referer,
        @NotBlank String detailPath,
        @NotBlank String searchPath,
        @Positive int bucketThreshold,
        @Positive int pageSize,
        @Positive int maxAttempts,
        @NotNull Duration initialBackoff,
        @NotNull Duration requestDelay) {
}
