package com.novi.dto.library;

import com.novi.entity.enums.ReadingStatus;
import jakarta.validation.constraints.NotNull;

public record AddBookRequest(
        @NotNull Long bookId,
        ReadingStatus status // optional, defaults to WANT_TO_READ
) {
}
