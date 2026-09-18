package com.novi.dto.shelf;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShelfRequest(
        @NotBlank @Size(max = 100) String name
) {
}
