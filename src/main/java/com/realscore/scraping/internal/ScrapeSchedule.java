package com.realscore.scraping.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.scheduling.support.CronExpression;

/**
 * The configured scrape cron in Europe/Prague time, evaluated against wall-clock instants. {@code -} disables it.
 */
final class ScrapeSchedule {

    static final String DISABLED = "-";
    static final ZoneId ZONE = ZoneId.of("Europe/Prague");

    private final CronExpression cron;

    ScrapeSchedule(String cron) {
        this.cron = DISABLED.equals(cron) ? null : CronExpression.parse(cron);
    }

    boolean isEnabled() {
        return cron != null;
    }

    /** @return the first slot strictly after {@code after}, or empty if disabled or the cron never fires again */
    Optional<Instant> nextSlotAfter(Instant after) {
        if (cron == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cron.next(ZonedDateTime.ofInstant(after, ZONE))).map(ZonedDateTime::toInstant);
    }
}
