package com.novi.service;

import com.novi.dto.history.ReadingHistoryResponse;
import com.novi.entity.Book;
import com.novi.entity.ReadingHistoryEvent;
import com.novi.entity.User;
import com.novi.entity.enums.HistoryEventType;
import com.novi.repository.ReadingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Append-only log of real user actions. This will become a key input to the
 * Phase 2 AI recommendation engine, so events are only ever recorded in
 * response to a genuine action - never fabricated or backfilled.
 */
@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;

    @Transactional
    public void record(User user, Book book, HistoryEventType eventType) {
        ReadingHistoryEvent event = ReadingHistoryEvent.builder()
                .user(user)
                .book(book)
                .eventType(eventType)
                .build();
        readingHistoryRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<ReadingHistoryResponse> getHistory(User user) {
        return readingHistoryRepository.findByUserOrderByOccurredAtDesc(user).stream()
                .map(e -> new ReadingHistoryResponse(
                        e.getId(),
                        e.getBook().getId(),
                        e.getBook().getTitle(),
                        e.getEventType(),
                        e.getOccurredAt()
                ))
                .toList();
    }
}
