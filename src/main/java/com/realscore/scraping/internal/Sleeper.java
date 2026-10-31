package com.realscore.scraping.internal;

import java.time.Duration;

/**
 * Waits for a while; lets tests replace real sleeping.
 */
@FunctionalInterface
interface Sleeper {

    void sleep(Duration duration) throws InterruptedException;
}
