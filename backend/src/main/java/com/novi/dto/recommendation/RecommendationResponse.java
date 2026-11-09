package com.novi.dto.recommendation;

import java.util.List;

/**
 * A single ranked recommendation returned to the frontend. Carries everything
 * needed to render a book card and detail view, plus the interpretable
 * relevance score and the grounded AI explanation of why Novi picked it.
 */
public record RecommendationResponse(
        Long bookId,
        String title,
        List<String> authors,
        String coverImageUrl,
        String description,
        List<String> genres,
        List<String> themes,
        int relevanceScore,
        String explanation
) {
}
