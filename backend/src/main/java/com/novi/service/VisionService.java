package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns a photo into a list of books the vision model believes it can see.
 * This is the ONLY class that knows how to prompt a vision-language model and
 * parse its output, so the model can be swapped later without touching the
 * scan orchestration. It deliberately does NOT touch the catalog or database -
 * matching detected text to real books is {@link BookMatchingService}'s job.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisionService {

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    private static final int MAX_SHELF_BOOKS = 60;

    /** What the model read for one book, plus how sure it says it is (0.0-1.0). */
    public record DetectedBook(String title, String author, double confidence) {
    }

    public boolean isAvailable() {
        return anthropicClient.isAvailable();
    }

    private static final String SINGLE_SYSTEM_PROMPT = """
            You are a book-identification assistant. You are shown a photo of a
            single physical book (its cover or spine). Identify the book.
            Respond with ONLY a JSON object, no prose, of the form:
            {"title": "...", "author": "...", "confidence": 0.0-1.0}
            confidence is how sure you are that you read the title/author text
            correctly. If you genuinely cannot read any book, respond with
            {"title": null, "author": null, "confidence": 0}.""";

    private static final String SHELF_SYSTEM_PROMPT = """
            You are a book-identification assistant. You are shown a photo of a
            bookshelf with multiple books. Read each spine you can and list the
            books. Respond with ONLY a JSON object, no prose, of the form:
            {"books": [{"title": "...", "author": "...", "confidence": 0.0-1.0}, ...]}
            Include one entry per distinct book you can read. confidence is how
            sure you are you read that spine correctly. Omit books whose text you
            cannot read at all rather than guessing wildly. If you can read no
            books, respond with {"books": []}.""";

    /** Detect the single book in a photo. Empty if vision is off or nothing was read. */
    public Optional<DetectedBook> detectSingleBook(String base64Image, String mediaType) {
        return anthropicClient
                .completeWithImage(SINGLE_SYSTEM_PROMPT,
                        "Identify this book. Return only the JSON object.",
                        base64Image, mediaType, 300)
                .flatMap(this::parseSingle);
    }

    /** Detect every readable book on a shelf. Empty list if vision is off or nothing was read. */
    public List<DetectedBook> detectShelfBooks(String base64Image, String mediaType) {
        return anthropicClient
                .completeWithImage(SHELF_SYSTEM_PROMPT,
                        "List every book you can read on this shelf. Return only the JSON object.",
                        base64Image, mediaType, 2000)
                .map(this::parseShelf)
                .orElseGet(List::of);
    }

    private Optional<DetectedBook> parseSingle(String raw) {
        try {
            JsonNode node = objectMapper.readTree(AnthropicClient.stripJsonFences(raw));
            return toDetected(node);
        } catch (Exception e) {
            log.warn("Failed to parse single-book vision response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<DetectedBook> parseShelf(String raw) {
        try {
            JsonNode root = objectMapper.readTree(AnthropicClient.stripJsonFences(raw));
            JsonNode books = root.path("books");
            if (!books.isArray()) {
                return List.of();
            }
            List<DetectedBook> results = new ArrayList<>();
            for (JsonNode node : books) {
                toDetected(node).ifPresent(results::add);
                if (results.size() >= MAX_SHELF_BOOKS) {
                    break;
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Failed to parse shelf vision response: {}", e.getMessage());
            return List.of();
        }
    }

    private Optional<DetectedBook> toDetected(JsonNode node) {
        String title = text(node.get("title"));
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        String author = text(node.get("author"));
        double confidence = clamp(node.path("confidence").asDouble(0.5));
        return Optional.of(new DetectedBook(title.trim(), author == null ? "" : author.trim(), confidence));
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }
}
