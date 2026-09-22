package com.novi.controller;

import com.novi.dto.book.BookResponse;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.common.PageResponse;
import com.novi.dto.review.ReviewResponse;
import com.novi.service.BookEmbeddingService;
import com.novi.service.BookService;
import com.novi.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookService bookService;
    private final ReviewService reviewService;
    private final BookEmbeddingService bookEmbeddingService;

    /**
     * Browse the local catalog, or - when {@code q} is given - search the
     * external provider. Both branches return the same {@link PageResponse}
     * envelope so clients get one consistent list shape. Live search results
     * aren't paginated at the source, so they come back as a single page.
     */
    @GetMapping
    public PageResponse<BookSummaryResponse> browseOrSearch(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (q != null && !q.isBlank()) {
            return PageResponse.ofSinglePage(bookService.search(q));
        }
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageResponse.from(bookService.browse(Math.max(0, page), boundedSize));
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable Long id) {
        return bookService.getById(id);
    }

    @GetMapping("/{id}/reviews")
    public PageResponse<ReviewResponse> getReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        PageRequest pageable = PageRequest.of(Math.max(0, page), boundedSize, Sort.by("createdAt").descending());
        return reviewService.getForBook(id, pageable);
    }

    /**
     * Backfills embeddings for any book imported before the AI providers were
     * configured (or before Phase 2 existed at all). Restricted to admins
     * (see SecurityConfig / {@code novi.security.admin-usernames}) because it
     * can trigger a large number of outbound embedding calls.
     */
    @PostMapping("/backfill-embeddings")
    public java.util.Map<String, Integer> backfillEmbeddings(@RequestParam(defaultValue = "200") int limit) {
        int processed = bookEmbeddingService.backfillMissingEmbeddings(Math.max(1, Math.min(limit, 200)));
        return java.util.Map.of("processed", processed);
    }
}
