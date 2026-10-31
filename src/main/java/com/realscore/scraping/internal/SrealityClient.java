package com.realscore.scraping.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
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
    private final String detailPath;
    private final String searchPath;

    public SrealityClient(SrealityProperties properties) {
        this.detailPath = properties.detailPath();
        this.searchPath = properties.searchPath();
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.REFERER, properties.referer())
                .build();
    }

    /**
     * Returns the detail response body exactly as received, including the {@code {"result": ...}} wrapper.
     *
     * @throws SrealityApiException if the response status is not 200
     */
    public String fetchEstateDetail(long estateId) {
        return restClient.get()
                .uri(detailPath, estateId)
                .retrieve()
                .onStatus(status -> status.value() != 200, (request, response) -> {
                    throw new SrealityApiException(estateId, request.getURI().getPath(), response.getStatusCode().value());
                })
                .body(String.class);
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
     * Returns one page of estate IDs matching the given search params, plus the total match count.
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
        List<Long> estateIds = response.results() == null ? List.of()
                : response.results().stream().map(SearchResponse.Result::hashId).toList();
        return new SearchPage(response.pagination().total(), estateIds);
    }

    private record SearchResponse(Pagination pagination, List<Result> results) {

        private record Pagination(Long total) {}

        private record Result(@JsonProperty("hash_id") long hashId) {}
    }
}
