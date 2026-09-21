package com.novi.controller;

import com.novi.dto.scan.ConfirmScanRequest;
import com.novi.dto.scan.ConfirmScanResponse;
import com.novi.dto.scan.ScanResultResponse;
import com.novi.exception.BadRequestException;
import com.novi.security.CurrentUserProvider;
import com.novi.service.BookScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * Phase 3 computer-vision book scanner. Users upload a photo of a single book
 * or a bookshelf; Novi detects the books and returns them (with confidence
 * scores) for review. A separate confirm step then adds the chosen books to
 * the library - nothing is added automatically.
 */
@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final BookScanService bookScanService;
    private final CurrentUserProvider currentUserProvider;

    /** Scan a single physical book from one photo. */
    @PostMapping("/book")
    public ScanResultResponse scanBook(@RequestParam("image") MultipartFile image) throws IOException {
        return bookScanService.scanSingleBook(
                currentUserProvider.getCurrentUser(), image.getBytes(), validateAndGetType(image));
    }

    /** Scan a whole bookshelf from one photo. */
    @PostMapping("/shelf")
    public ScanResultResponse scanShelf(@RequestParam("image") MultipartFile image) throws IOException {
        return bookScanService.scanShelf(
                currentUserProvider.getCurrentUser(), image.getBytes(), validateAndGetType(image));
    }

    /** Retrieve a previous scan (e.g. to resume confirming it). */
    @GetMapping("/sessions/{id}")
    public ScanResultResponse getSession(@PathVariable Long id) {
        return bookScanService.getSession(currentUserProvider.getCurrentUser(), id);
    }

    /** Apply the user's confirm/skip decisions, adding confirmed books to the library. */
    @PostMapping("/sessions/{id}/confirm")
    public ConfirmScanResponse confirm(@PathVariable Long id, @Valid @RequestBody ConfirmScanRequest request) {
        return bookScanService.confirm(currentUserProvider.getCurrentUser(), id, request);
    }

    private String validateAndGetType(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("No image was uploaded");
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image type. Use JPEG, PNG, WEBP or GIF.");
        }
        return contentType.toLowerCase();
    }
}
