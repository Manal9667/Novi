package com.novi.dto.scan;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.entity.enums.ScanCandidateStatus;

/**
 * One detected book returned to the client for confirmation. {@code matchedBook}
 * is null when the detected text couldn't be resolved to a catalog book, and
 * {@code confidence} lets the UI flag uncertain matches ("Possible match: 73%").
 */
public record ScanCandidateResponse(
        Long id,
        String detectedTitle,
        String detectedAuthor,
        double confidence,
        BookSummaryResponse matchedBook,
        ScanCandidateStatus status
) {
}
