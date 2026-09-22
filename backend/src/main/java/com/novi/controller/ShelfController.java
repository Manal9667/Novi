package com.novi.controller;

import com.novi.dto.common.PageResponse;
import com.novi.dto.shelf.ShelfRequest;
import com.novi.dto.shelf.ShelfResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.ShelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shelves")
@RequiredArgsConstructor
public class ShelfController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final ShelfService shelfService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PageResponse<ShelfResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return shelfService.getAll(currentUserProvider.getCurrentUser(), PageRequest.of(Math.max(0, page), boundedSize));
    }

    @PostMapping
    public ResponseEntity<ShelfResponse> create(@Valid @RequestBody ShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shelfService.create(currentUserProvider.getCurrentUser(), request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shelfService.delete(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/books/{bookId}")
    public ShelfResponse addBook(@PathVariable Long id, @PathVariable Long bookId) {
        return shelfService.addBook(currentUserProvider.getCurrentUser(), id, bookId);
    }

    @DeleteMapping("/{id}/books/{bookId}")
    public ShelfResponse removeBook(@PathVariable Long id, @PathVariable Long bookId) {
        return shelfService.removeBook(currentUserProvider.getCurrentUser(), id, bookId);
    }
}
