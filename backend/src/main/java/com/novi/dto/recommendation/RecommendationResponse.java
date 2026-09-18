package com.novi.dto.recommendation;

import com.novi.dto.book.BookSummaryResponse;

import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        Long id,
        BookSummaryResponse book,
        int matchPercent,
        List<String> reasons,
        String potentialDownside,
        Instant createdAt
) {
}
