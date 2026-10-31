package com.realscore.scraping.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the scheduled scrape; values live in {@code application.properties} under {@code realscore.scrape}.
 *
 * @param cron Spring cron expression for {@link SrealityScrapeJob}, in Europe/Prague time; {@code -} disables it
 * @param priceCap maximum price in CZK of the sale apartments collected
 */
@Validated
@ConfigurationProperties("realscore.scrape")
public record ScrapeJobProperties(@NotBlank String cron, @Positive long priceCap) {
}
