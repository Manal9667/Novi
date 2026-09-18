package com.novi.dto.library;

import com.novi.entity.enums.ReadingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull ReadingStatus status
) {
}
