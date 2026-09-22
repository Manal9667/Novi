package com.novi.controller;

import com.novi.dto.common.PageResponse;
import com.novi.dto.library.AddBookRequest;
import com.novi.dto.library.UpdateStatusRequest;
import com.novi.dto.library.UserBookResponse;
import com.novi.entity.enums.ReadingStatus;
import com.novi.security.CurrentUserProvider;
import com.novi.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    /** Generous default so typical libraries aren't silently truncated, while still bounding the response. */
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final LibraryService libraryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PageResponse<UserBookResponse> getLibrary(
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return libraryService.getLibrary(
                currentUserProvider.getCurrentUser(), status, PageRequest.of(Math.max(0, page), boundedSize));
    }

    @PostMapping("/books")
    public ResponseEntity<UserBookResponse> addBook(@Valid @RequestBody AddBookRequest request) {
        UserBookResponse response = libraryService.addBook(
                currentUserProvider.getCurrentUser(), request.bookId(), request.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> removeBook(@PathVariable Long bookId) {
        libraryService.removeBook(currentUserProvider.getCurrentUser(), bookId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/books/{bookId}/status")
    public UserBookResponse updateStatus(@PathVariable Long bookId, @Valid @RequestBody UpdateStatusRequest request) {
        return libraryService.updateStatus(currentUserProvider.getCurrentUser(), bookId, request.status());
    }
}
