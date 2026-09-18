package com.novi.dto.library;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.entity.enums.ReadingStatus;

import java.time.Instant;

public record UserBookResponse(
        Long id,
        BookSummaryResponse book,
        ReadingStatus status,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {
}
