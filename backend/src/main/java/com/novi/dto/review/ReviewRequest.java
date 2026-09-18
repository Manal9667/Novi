package com.novi.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank @Size(max = 5000) String content
) {
}
