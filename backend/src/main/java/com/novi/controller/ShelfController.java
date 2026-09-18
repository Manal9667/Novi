package com.novi.controller;

import com.novi.dto.shelf.ShelfRequest;
import com.novi.dto.shelf.ShelfResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.ShelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelves")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<ShelfResponse> getAll() {
        return shelfService.getAll(currentUserProvider.getCurrentUser());
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
