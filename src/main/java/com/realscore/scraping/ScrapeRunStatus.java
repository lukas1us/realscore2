package com.realscore.scraping;

public enum ScrapeRunStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    /** The app stopped while the run was {@link #RUNNING}; set when the app is running again. */
    ABORTED
}
