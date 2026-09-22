package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thin adapter around the Open Library public API. This is intentionally the
 * ONLY class that knows about Open Library's response shape, so a different
 * metadata provider can be swapped in later behind {@link BookService}
 * without touching the rest of the application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenLibraryService {

    private final RestClient openLibraryRestClient;

    public record ExternalBook(
            String externalId,
            String title,
            String coverImageUrl,
            String isbn,
            String publicationDate,
            List<String> authorNames
    ) {
    }

    /**
     * Richer, per-work metadata that the search endpoint doesn't return.
     * Fetched lazily (once, on first import) from the Open Library "work"
     * document so book detail pages have a real description and books carry
     * genres/subjects the taste profile and recommender can key off.
     */
    public record WorkDetails(
            String description,
            List<String> subjects
    ) {
    }

    public List<ExternalBook> search(String query, int limit) {
        try {
            JsonNode root = openLibraryRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search.json")
                            .queryParam("q", query)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            List<ExternalBook> results = new ArrayList<>();
            if (root == null || !root.has("docs")) {
                return results;
            }

            for (JsonNode doc : root.get("docs")) {
                String key = doc.path("key").asText(null); // e.g. "/works/OL45804W"
                if (key == null) continue;

                List<String> authors = new ArrayList<>();
                if (doc.has("author_name")) {
                    doc.get("author_name").forEach(a -> authors.add(a.asText()));
                }

                String coverId = doc.has("cover_i") ? doc.get("cover_i").asText(null) : null;
                String coverUrl = coverId != null
                        ? "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg"
                        : null;

                String isbn = null;
                if (doc.has("isbn") && doc.get("isbn").size() > 0) {
                    isbn = doc.get("isbn").get(0).asText();
                }

                String firstPublishYear = doc.has("first_publish_year")
                        ? doc.get("first_publish_year").asText(null) + "-01-01"
                        : null;

                results.add(new ExternalBook(
                        key,
                        doc.path("title").asText("Untitled"),
                        coverUrl,
                        isbn,
                        firstPublishYear,
                        authors
                ));
            }
            return results;
        } catch (Exception e) {
            log.warn("Open Library search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches the description and subjects for a single work. The search
     * endpoint returns neither, so this is called once when a book is first
     * imported. Best-effort: any failure returns empty so importing a book
     * never breaks just because the extra metadata couldn't be loaded.
     *
     * @param workKey the Open Library work key, e.g. {@code /works/OL45804W}
     */
    public Optional<WorkDetails> fetchWorkDetails(String workKey) {
        if (workKey == null || workKey.isBlank()) {
            return Optional.empty();
        }
        try {
            // workKey already starts with "/works/..."; the work document lives at
            // that path + ".json" relative to the configured base URL.
            JsonNode root = openLibraryRestClient.get()
                    .uri(workKey + ".json")
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return Optional.empty();
            }

            return Optional.of(new WorkDetails(extractDescription(root), extractSubjects(root)));
        } catch (Exception e) {
            log.warn("Open Library work-details fetch failed for '{}': {}", workKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Open Library represents a description either as a plain string or as a
     * {@code {"type": "/type/text", "value": "..."}} object depending on the
     * record's age. Handle both shapes.
     */
    private String extractDescription(JsonNode root) {
        JsonNode descNode = root.get("description");
        if (descNode == null || descNode.isNull()) {
            return null;
        }
        String description = descNode.isTextual() ? descNode.asText() : descNode.path("value").asText(null);
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private List<String> extractSubjects(JsonNode root) {
        List<String> subjects = new ArrayList<>();
        if (root.has("subjects")) {
            for (JsonNode subject : root.get("subjects")) {
                String value = subject.asText(null);
                if (value != null && !value.isBlank()) {
                    subjects.add(value.trim());
                }
            }
        }
        return subjects;
    }
}
