package com.realscore.parsing;

import java.math.BigDecimal;
import java.time.Instant;

/** One asking price of a listing, from the first snapshot (by fetch time) that carried it. */
public record PricePoint(Instant observedAt, BigDecimal priceCzk) {
}
