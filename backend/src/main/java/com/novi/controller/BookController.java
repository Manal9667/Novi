package com.novi.controller;

import com.novi.dto.book.BookResponse;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.review.ReviewResponse;
import com.novi.service.BookEmbeddingService;
import com.novi.service.BookService;
import com.novi.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;
    private final BookEmbeddingService bookEmbeddingService;

    @GetMapping
    public Object browseOrSearch(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (q != null && !q.isBlank()) {
            List<BookSummaryResponse> results = bookService.search(q);
            return results;
        }
        Page<BookSummaryResponse> results = bookService.browse(page, size);
        return results;
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable Long id) {
        return bookService.getById(id);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> getReviews(@PathVariable Long id) {
        return reviewService.getForBook(id);
    }

    /**
     * Backfills embeddings for any book imported before the AI providers were
     * configured (or before Phase 2 existed at all). Any authenticated user
     * can trigger this in the portfolio deployment; lock it down to an admin
     * role before shipping this beyond a demo.
     */
    @PostMapping("/backfill-embeddings")
    public java.util.Map<String, Integer> backfillEmbeddings(@RequestParam(defaultValue = "200") int limit) {
        int processed = bookEmbeddingService.backfillMissingEmbeddings(Math.max(1, Math.min(limit, 200)));
        return java.util.Map.of("processed", processed);
    }
}
