package com.novi.entity.enums;

/**
 * Lifecycle of a single detected book within a scan. A candidate is never
 * silently added to a library - it stays PENDING until the user explicitly
 * confirms it (CONFIRMED) or dismisses it (SKIPPED).
 */
public enum ScanCandidateStatus {
    PENDING,
    CONFIRMED,
    SKIPPED
}
