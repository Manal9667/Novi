package com.novi.dto.history;

import com.novi.entity.enums.HistoryEventType;

import java.time.Instant;

public record ReadingHistoryResponse(
        Long id,
        Long bookId,
        String bookTitle,
        HistoryEventType eventType,
        Instant occurredAt
) {
}
