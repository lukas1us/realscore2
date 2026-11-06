package com.realscore.scraping.internal;

/**
 * What {@link SrealityRemovalCheck} did with the active listings a run did not see.
 *
 * @param checked active listings the run did not see and that were due a check, so their detail was fetched
 * @param kept checked listings whose detail was still there (HTTP 200); they stay active, marked out of scope
 * @param changed kept listings whose payload changed, so a new snapshot was written
 * @param removed checked listings whose detail is gone (HTTP 404 or 410), now marked removed
 * @param failed checked listings answering 403 or failing in any other way; left active, checked again next run
 */
public record RemovalCheckResult(int checked, int kept, int changed, int removed, int failed) {
}
