package com.novi.dto.rating;

import java.time.Instant;

public record RatingResponse(
        Long id,
        Long bookId,
        Integer stars,
        Instant createdAt,
        Instant updatedAt
) {
}
