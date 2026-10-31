package com.realscore.scraping.internal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches raw estate JSON from the Sreality v1 API (see Backlog doc "Sreality API").
 */
@Component
public class SrealityClient {

    private final RestClient restClient;

    public SrealityClient(SrealityProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.REFERER, "https://www.sreality.cz/")
                .build();
    }

    /**
     * Returns the detail response body exactly as received, including the {@code {"result": ...}} wrapper.
     *
     * @throws SrealityApiException if the response status is not 200
     */
    public String fetchEstateDetail(long estateId) {
        return restClient.get()
                .uri("/api/v1/estates/{id}", estateId)
                .retrieve()
                .onStatus(status -> status.value() != 200, (request, response) -> {
                    throw new SrealityApiException(estateId, response.getStatusCode().value());
                })
                .body(String.class);
    }
}
