package com.novi.dto.shelf;

import com.novi.dto.book.BookSummaryResponse;

import java.time.Instant;
import java.util.List;

public record ShelfResponse(
        Long id,
        String name,
        Instant createdAt,
        List<BookSummaryResponse> books
) {
}
