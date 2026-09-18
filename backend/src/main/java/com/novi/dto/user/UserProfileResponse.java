package com.novi.dto.user;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String username,
        String displayName,
        String bio,
        Instant createdAt,
        long booksRead,
        long currentlyReading,
        long wantToRead,
        long reviewCount
) {
}
