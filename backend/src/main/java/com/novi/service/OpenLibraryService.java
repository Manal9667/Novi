package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

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
}
