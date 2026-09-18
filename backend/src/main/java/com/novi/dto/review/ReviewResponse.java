package com.novi.dto.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookId,
        Long userId,
        String username,
        String displayName,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
