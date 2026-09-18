package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.entity.Book;
import com.novi.entity.Theme;
import com.novi.repository.BookRepository;
import com.novi.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns thematic tags (e.g. "Political", "Coming of age", "Found family")
 * to a book by asking Claude to read its description - themes are more
 * nuanced than a fixed genre taxonomy and are exactly the kind of judgment
 * call an LLM is well-suited for, per the project brief.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookThemeTaggingService {

    private static final String SYSTEM_PROMPT = """
            You are a literary taxonomist. Given a book's title, author, genres
            and description, identify 3 to 6 recurring THEMES (not genres) that
            characterize it - e.g. "Political intrigue", "Coming of age",
            "Found family", "Psychological tension", "Betrayal", "Redemption".
            Respond with ONLY a JSON array of short theme strings, nothing else.
            Example: ["Political intrigue", "Coming of age", "Betrayal"]
            """;

    private final AnthropicClient anthropicClient;
    private final ThemeRepository themeRepository;
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void ensureThemes(Book book) {
        if (!book.getThemes().isEmpty() || !anthropicClient.isAvailable()) {
            return;
        }
        if (book.getDescription() == null || book.getDescription().isBlank()) {
            return;
        }

        String prompt = "Title: " + book.getTitle()
                + "\nGenres: " + book.getGenres().stream().map(g -> g.getName()).reduce((a, b) -> a + ", " + b).orElse("")
                + "\nDescription: " + book.getDescription();

        anthropicClient.complete(SYSTEM_PROMPT, prompt, 300).ifPresent(raw -> {
            try {
                JsonNode array = objectMapper.readTree(AnthropicClient.stripJsonFences(raw));
                if (!array.isArray()) return;

                for (JsonNode node : array) {
                    String name = node.asText().trim();
                    if (name.isBlank()) continue;
                    Theme theme = themeRepository.findByNameIgnoreCase(name)
                            .orElseGet(() -> themeRepository.save(Theme.builder().name(name).build()));
                    book.getThemes().add(theme);
                }
                bookRepository.save(book);
            } catch (Exception e) {
                log.warn("Failed to parse theme-tagging response for book {}: {}", book.getId(), e.getMessage());
            }
        });
    }
}
