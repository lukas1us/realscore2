package com.realscore.scraping;

/**
 * The counts {@link ScrapeRunService#updateCounts} stores on a {@code scrape_run}.
 *
 * @param found estate IDs the run's search returned
 * @param newCount new listings whose detail was fetched and recorded
 * @param updated known listings whose refetched payload changed
 * @param removed listings marked removed because the run didn't see them
 * @param skipped IDs Sreality answered 403/404 for; nothing recorded
 * @param alreadyKnown found IDs that already had a listing
 * @param errors IDs whose fetch failed in any other way
 */
public record ScrapeRunCounts(
        int found, int newCount, int updated, int removed, int skipped, int alreadyKnown, int errors) {
}
