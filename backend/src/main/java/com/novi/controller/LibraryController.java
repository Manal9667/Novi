package com.novi.controller;

import com.novi.dto.library.AddBookRequest;
import com.novi.dto.library.UpdateStatusRequest;
import com.novi.dto.library.UserBookResponse;
import com.novi.entity.enums.ReadingStatus;
import com.novi.security.CurrentUserProvider;
import com.novi.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<UserBookResponse> getLibrary(@RequestParam(required = false) ReadingStatus status) {
        return libraryService.getLibrary(currentUserProvider.getCurrentUser(), status);
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
