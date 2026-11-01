package com.realscore.analysis;

/** How fine a locality is, finest first. Each level's parent is the next one. */
public enum LocalityLevel {
    CITY_PART,
    CITY,
    DISTRICT,
    REGION
}
