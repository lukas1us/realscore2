package com.realscore.scraping.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the scheduled scrape; values live in {@code application.properties} under {@code realscore.scrape}.
 *
 * @param cron Spring cron expression for {@link SrealityScrapeJob}, in Europe/Prague time; {@code -} disables it.
 *             A slot that passed without a run (host asleep, app down) is caught up, see {@link ScrapeScheduler}
 * @param checkInterval how often {@link ScrapeScheduler} checks whether a run is due, so runs start up to this long
 *                      after their slot
 * @param priceCap maximum price in CZK of the sale apartments collected
 * @param rentPriceCap maximum monthly rent in CZK of the rental apartments collected, or {@code null} (the default,
 *                     an empty {@code SCRAPE_RENT_PRICE_CAP}) for no cap
 * @param refetchAfter refetch strategy for known listings: one that a run collects is fetched again once its last
 *                     fetch is older than this, so a change is captured within roughly this long; {@code 0} refetches
 *                     every known listing on every run, at the cost of one detail request each
 * @param outOfScopeRecheckAfter a listing marked out of scope (online, but no longer returned by the searches) has
 *                               its detail checked again by {@link SrealityRemovalCheck} only once its last fetch is
 *                               older than this; {@code 0} checks it on every run
 */
@Validated
@ConfigurationProperties("realscore.scrape")
public record ScrapeJobProperties(
        @NotBlank String cron,
        @NotNull Duration checkInterval,
        @Positive long priceCap,
        @Positive Long rentPriceCap,
        @NotNull Duration refetchAfter,
        @NotNull Duration outOfScopeRecheckAfter) {
}
