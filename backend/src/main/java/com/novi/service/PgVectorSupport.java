package com.novi.service;

import com.novi.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Detects (once, then caches) whether the running database actually has the
 * native pgvector column. The V6 migration only creates {@code books.embedding_vec}
 * when the pgvector extension is available, so on a plain PostgreSQL the column
 * is absent. Consulting this before issuing any vector SQL lets the app use
 * index-backed pgvector retrieval where possible and fall back cleanly
 * everywhere else - without ever erroring on a missing column (which would also
 * poison the surrounding transaction).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PgVectorSupport {

    private final BookRepository bookRepository;

    private volatile Boolean available;

    public boolean isAvailable() {
        Boolean cached = available;
        if (cached == null) {
            synchronized (this) {
                if (available == null) {
                    boolean present;
                    try {
                        present = bookRepository.isEmbeddingVectorColumnPresent();
                    } catch (Exception e) {
                        present = false;
                    }
                    log.info("pgvector native retrieval {}", present ? "enabled" : "unavailable - using application-layer fallback");
                    available = present;
                }
                cached = available;
            }
        }
        return cached;
    }
}
