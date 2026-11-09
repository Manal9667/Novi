package com.novi.controller;

import com.novi.dto.book.BookResponse;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.common.PageResponse;
import com.novi.service.BookEmbeddingService;
import com.novi.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookService bookService;
    private final BookEmbeddingService bookEmbeddingService;

    /**
     * Browse the seeded catalog. Public and side-effect-free.
     */
    @GetMapping
    public PageResponse<BookSummaryResponse> browse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageResponse.from(bookService.browse(Math.max(0, page), boundedSize));
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable Long id) {
        return bookService.getById(id);
    }

    /**
     * Backfills embeddings for any seeded book that doesn't yet have one.
     * Restricted to admins (see SecurityConfig / {@code novi.security.admin-usernames})
     * because it can trigger a batch of outbound embedding calls.
     */
    @PostMapping("/backfill-embeddings")
    public java.util.Map<String, Integer> backfillEmbeddings(@RequestParam(defaultValue = "200") int limit) {
        int processed = bookEmbeddingService.backfillMissingEmbeddings(Math.max(1, Math.min(limit, 500)));
        return java.util.Map.of("processed", processed);
    }
}
