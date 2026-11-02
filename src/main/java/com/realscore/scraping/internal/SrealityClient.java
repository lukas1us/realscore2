package com.realscore.scraping.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches raw estate JSON from the Sreality v1 API (see Backlog doc "Sreality API").
 */
@Slf4j
@Component
public class SrealityClient {

    private final RestClient restClient;
    private final String detailPath;
    private final String searchPath;

    SrealityClient(SrealityProperties properties, SrealityRequestPolicy requestPolicy) {
        this.detailPath = properties.detailPath();
        this.searchPath = properties.searchPath();
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.REFERER, properties.referer())
                .requestInterceptor(requestPolicy)
                .build();
    }

    /**
     * Returns the detail response body as received, including the {@code {"result": ...}} wrapper but without the
     * seller's contact data ({@link SrealityPayloadScrubber}), or empty if
     * Sreality answers 403, 404 or 410 (the estate is gone or hidden: a skip, not an error).
     *
     * @throws SrealityApiException if the final response status is anything else but 200
     * @throws IllegalStateException if a 200 response has an empty or blank body
     */
    public Optional<String> fetchEstateDetail(long estateId) {
        return fetchDetail(estateId) instanceof EstateDetail.Found(String body) ? Optional.of(body) : Optional.empty();
    }

    /**
     * Fetches one estate's detail and tells a gone estate (404, 410) apart from a hidden one (403). A found body has
     * the seller's contact data removed ({@link SrealityPayloadScrubber}).
     *
     * @throws SrealityApiException if the final response status is anything else but 200, 403, 404 or 410
     * @throws IllegalStateException if a 200 response has an empty or blank body, or one that is not valid JSON
     */
    public EstateDetail fetchDetail(long estateId) {
        return restClient.get()
                .uri(detailPath, estateId)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (status == 404 || status == 410) {
                        log.info("Sreality estate {} is gone: HTTP {}", estateId, status);
                        return new EstateDetail.Gone(status);
                    }
                    if (status == 403) {
                        log.info("Sreality estate {} is forbidden: HTTP 403", estateId);
                        return new EstateDetail.Forbidden();
                    }
                    if (status != 200) {
                        throw new SrealityApiException(estateId, request.getURI().getPath(), status);
                    }
                    String body = response.bodyTo(String.class);
                    if (body == null || body.isBlank()) {
                        throw new IllegalStateException("Sreality returned an empty body for estate " + estateId);
                    }
                    return new EstateDetail.Found(SrealityPayloadScrubber.scrub(body));
                });
    }

    /**
     * Returns how many estates match the given search params, without fetching any of them ({@code limit=0}).
     *
     * @param searchParams v1 search query params, e.g. {@code locality_region_id -> "10"}
     * @throws SrealityApiException if the response status is not 200
     * @throws IllegalStateException if a 200 response has no {@code pagination.total}
     */
    public long countEstates(Map<String, String> searchParams) {
        return searchEstates(searchParams, 0, 0).total();
    }

    /**
     * Returns one page of estates (ID and search price) matching the given search params, plus the total match
     * count.
     *
     * @param searchParams v1 search query params, e.g. {@code locality_region_id -> "10"}
     * @throws SrealityApiException if the response status is not 200
     * @throws IllegalStateException if a 200 response has no {@code pagination.total}
     */
    public SearchPage searchEstates(Map<String, String> searchParams, int limit, int offset) {
        SearchResponse response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(searchPath);
                    searchParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.queryParam("limit", limit).queryParam("offset", offset).build();
                })
                .retrieve()
                .onStatus(status -> status.value() != 200, (request, errorResponse) -> {
                    throw new SrealityApiException(request.getURI().getPath(), errorResponse.getStatusCode().value());
                })
                .body(SearchResponse.class);
        if (response == null || response.pagination() == null || response.pagination().total() == null) {
            throw new IllegalStateException("Sreality search response has no pagination.total");
        }
        List<SearchPage.Result> results = response.results() == null ? List.of()
                : response.results().stream()
                        .map(r -> new SearchPage.Result(r.hashId(), r.priceSummaryCzk()))
                        .toList();
        return new SearchPage(response.pagination().total(), results);
    }

    private record SearchResponse(Pagination pagination, List<Result> results) {

        private record Pagination(Long total) {}

        private record Result(@JsonProperty("hash_id") long hashId,
                @JsonProperty("price_summary_czk") BigDecimal priceSummaryCzk) {}
    }
}
