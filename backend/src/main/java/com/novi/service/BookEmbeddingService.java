package com.novi.service;

import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Generates and stores the semantic embedding for a book, from whatever
 * metadata is available (title, description, genres, authors). This is the
 * ONLY class that decides what text represents a book for embedding purposes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookEmbeddingService {

    private final EmbeddingService embeddingService;
    private final BookRepository bookRepository;
    private final PgVectorSupport pgVectorSupport;

    @Transactional
    public void ensureEmbedding(Book book) {
        if (book.getEmbedding() != null || !embeddingService.isAvailable()) {
            return;
        }

        String representation = buildRepresentation(book);
        embeddingService.embed(representation).ifPresent(vector -> {
            String json = VectorUtils.toJson(vector);
            book.setEmbedding(json);
            book.setEmbeddingModel("voyage");
            book.setEmbeddingUpdatedAt(Instant.now());
            Book saved = bookRepository.save(book);
            // Only touch the native vector column when it actually exists,
            // otherwise the failed UPDATE would poison this transaction and roll
            // back the embedding we just saved.
            if (pgVectorSupport.isAvailable()) {
                syncVectorColumn(saved.getId(), json);
            }
        });
    }

    /**
     * Mirrors the JSON embedding into the native {@code pgvector} column used
     * for index-backed nearest-neighbour retrieval. The JSON float-array text
     * is already valid pgvector input, so a direct cast populates the column.
     * Best-effort: a failure here (e.g. a dimension mismatch, or a database
     * without the vector extension) must not break book import - the JSON
     * embedding remains the source of truth and the app-layer cosine path still
     * works.
     */
    private void syncVectorColumn(Long bookId, String embeddingJson) {
        try {
            bookRepository.updateEmbeddingVector(bookId, embeddingJson);
        } catch (Exception e) {
            log.warn("Could not sync pgvector embedding_vec for book {}: {}", bookId, e.getMessage());
        }
    }

    /** Backfill embeddings for any book that doesn't have one yet. Safe to call repeatedly. */
    @Transactional
    public int backfillMissingEmbeddings(int maxBooks) {
        if (!embeddingService.isAvailable()) {
            log.info("Embedding provider not configured - skipping backfill");
            return 0;
        }
        List<Book> missing = bookRepository.findByEmbeddingIsNull();
        int processed = 0;
        for (Book book : missing) {
            if (processed >= maxBooks) break;
            ensureEmbedding(book);
            processed++;
        }
        return processed;
    }

    private String buildRepresentation(Book book) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(book.getTitle()).append("\n");

        String authors = book.getAuthors().stream().map(Author::getName).reduce((a, b) -> a + ", " + b).orElse("");
        if (!authors.isBlank()) sb.append("Author(s): ").append(authors).append("\n");

        String genres = book.getGenres().stream().map(Genre::getName).reduce((a, b) -> a + ", " + b).orElse("");
        if (!genres.isBlank()) sb.append("Genres: ").append(genres).append("\n");

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            sb.append("Description: ").append(book.getDescription());
        }
        return sb.toString();
    }

    public Optional<float[]> getVector(Book book) {
        return Optional.ofNullable(VectorUtils.fromJson(book.getEmbedding()));
    }
}
