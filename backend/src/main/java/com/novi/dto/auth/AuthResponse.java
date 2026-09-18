package com.novi.dto.auth;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String displayName
) {
}
