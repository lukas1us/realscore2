package com.realscore.scraping.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * A clock that only moves when asked to sleep or {@link #advance advanced}, and records every sleep.
 */
class FakeTime extends Clock implements Sleeper {

    private Instant now = Instant.parse("2026-10-31T12:00:00Z");

    @Getter
    private final List<Duration> sleeps = new ArrayList<>();

    @Override
    public void sleep(Duration duration) {
        sleeps.add(duration);
        now = now.plus(duration);
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }
}
