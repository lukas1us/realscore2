---
id: TASK-15
title: Shared rate limiting and retry policy for Sreality requests
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T23:43:00Z'
updated_date: '2026-10-31T20:24:00Z'
labels:
  - scraping
milestone: m-3
dependencies:
  - TASK-10
documentation:
  - doc-1
ordinal: 15000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Centralises the request pacing and failure handling the old app scattered across its scan jobs (see the 'Sreality API' doc's rate limiting section) into one policy used by every Sreality call, instead of each caller reimplementing its own backoff/skip rules.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 HTTP 429 responses are retried with exponential backoff up to a configurable max attempts
- [x] #2 HTTP 403 and 404 responses are not retried and are treated as a skip, not an error
- [x] #3 A configurable minimum delay is applied between requests
- [x] #4 Unit tests cover the 429-retry, 403/404-skip, and transient-error-retry branches independently
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. SrealityRequestPolicy (ClientHttpRequestInterceptor) on SrealityClient's RestClient: paces every attempt to request-delay since the previous one (shared, locked); retries 429, 5xx and IOException with ExponentialBackOff (initial-backoff, x2) up to max-attempts total; then returns the last response / rethrows. 403/404/other 4xx returned immediately. Sleeper + Clock injected for tests.
2. SrealityClient.fetchEstateDetail returns Optional<String>: 403/404 -> empty (logged INFO skip); other non-200 still SrealityApiException. Search keeps throwing on 403/404.
3. SrealityEstateFetcher.fetch returns boolean (false = skipped, nothing recorded).
4. Properties: max-attempts=3, initial-backoff=2s, request-delay=500ms (validated); test properties: 0ms delay, tiny backoff.
5. Tests: SrealityRequestPolicyTest (WireMock scenarios, fake sleeper/clock): 429->200, 429 exhausted, 403/404 not retried, connection reset->200, 503->200, min delay. SrealityClientTest/SrealityEstateFetcherTest updated for skips.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented as planned. SrealityRequestPolicy is a ClientHttpRequestInterceptor registered on SrealityClient's RestClient (bean in SrealityConfiguration with Thread::sleep + Clock.systemUTC()); retries 429/5xx/IOException with Spring ExponentialBackOff (x2), paces every attempt under a lock. fetchEstateDetail now uses exchange() and returns Optional<String> (403/404 -> empty, INFO log); SrealityEstateFetcher.fetch returns boolean. SrealityClient constructor is now package-private and takes the policy. New config max-attempts=3, initial-backoff=2s, request-delay=500ms. Tests: SrealityRequestPolicyTest (9, WireMock scenarios + FakeTime that records sleeps instead of sleeping), SrealityClientTest updated (403/404 skip, 410 still throws, 500 throws after 3 attempts), SrealityEstateFetcherTest gets a 404-skip case and 0ms delay / 1ms backoff via DynamicPropertySource. Shared test helpers FakeTime and SrealityTestSupport. Empty 200 detail body now NPEs inside the client (Optional.of) instead of in recordFetch: still TASK-29's job. mvn verify: 43 tests, BUILD SUCCESS.

Folded in TASK-29 (empty/blank detail body -> IllegalStateException) per user, which also resolves the 'argument might be null' warning on bodyTo. mvn verify: 46 tests.

Finalized via /finish-task: mvn verify green (46 tests). AC evidence: #1 SrealityRequestPolicyTest.tooManyRequestsIsRetriedAfterBackoff / tooManyRequestsOnEveryAttemptGivesUpWithDoublingBackoff (max-attempts in SrealityProperties); #2 SrealityRequestPolicyTest.forbiddenAndNotFoundAreSkippedWithoutRetry, SrealityEstateFetcherTest.goneEstateIsSkippedWithoutRecordingAnything; #3 SrealityRequestPolicyTest.requestsAreSpacedByAtLeastTheRequestDelay; #4 the above plus connectionErrorIsRetried, serverErrorIsRetried. No follow-up tasks created (Retry-After support offered, not requested).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added one pacing/retry policy for every Sreality request: SrealityRequestPolicy, a RestClient interceptor on SrealityClient. Each attempt waits until request-delay (500ms) has passed since the previous one; HTTP 429, 5xx and connection errors are retried with exponential backoff (2s, 4s, ...) up to max-attempts (3) in total, after which the last response or exception surfaces as before. 403/404 on a detail fetch are a skip: fetchEstateDetail returns Optional.empty() and SrealityEstateFetcher.fetch returns false without recording anything; searches still throw on 403/404. Settings are validated realscore.sreality.* properties. Tests use a fake clock/sleeper, so nothing really sleeps: SrealityRequestPolicyTest covers 429-retry, 429 exhausted, 403/404 no retry, connection-error retry, 5xx retry, search retry and pacing. mvn verify green, 43 tests. Not done: Retry-After header; empty detail body stays TASK-29.
<!-- SECTION:FINAL_SUMMARY:END -->
