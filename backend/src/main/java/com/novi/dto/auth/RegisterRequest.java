package com.novi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username may only contain letters, numbers and underscores")
        String username,

        @NotBlank
        @Size(min = 1, max = 64)
        String displayName,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        String confirmPassword
) {
}
