package com.novi.dto.recommendation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalLanguageRequest(
        @NotBlank @Size(max = 500) String query
) {
}
