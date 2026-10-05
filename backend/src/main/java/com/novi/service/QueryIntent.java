package com.novi.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A structured, internal representation of what a reader asked for in a
 * natural-language "Find Your Next Read" request. This never leaves the
 * backend - it only drives query-relevant retrieval and reranking. Produced by
 * {@link QueryIntentService} (via Gemini when available, or a deterministic
 * keyword fallback otherwise).
 */
public record QueryIntent(
        List<String> genres,
        List<String> themes,
        List<String> culturalContext,
        List<String> authors,
        List<String> similarTo,
        List<String> moods,
        List<String> keywords,
        String length
) {

    public QueryIntent {
        genres = safe(genres);
        themes = safe(themes);
        culturalContext = safe(culturalContext);
        authors = safe(authors);
        similarTo = safe(similarTo);
        moods = safe(moods);
        keywords = safe(keywords);
    }

    private static List<String> safe(List<String> in) {
        return in == null ? List.of() : in;
    }

    /**
     * The distinct textual terms used to drive lexical (keyword) retrieval
     * against book title/description/author/genre/theme. Combines every
     * concrete signal in the intent; mood/length are intentionally excluded
     * because they rarely appear verbatim in catalog metadata.
     */
    public List<String> retrievalTerms() {
        Set<String> terms = new LinkedHashSet<>();
        addAll(terms, culturalContext);
        addAll(terms, genres);
        addAll(terms, authors);
        addAll(terms, themes);
        addAll(terms, similarTo);
        addAll(terms, keywords);
        return new ArrayList<>(terms);
    }

    /**
     * A natural-language expansion of the request used to embed the query for
     * semantic retrieval. Falls back to the raw query when the intent is empty.
     */
    public String expandedQueryText(String rawQuery) {
        StringBuilder sb = new StringBuilder(rawQuery == null ? "" : rawQuery);
        appendClause(sb, "Genres", genres);
        appendClause(sb, "Cultural context", culturalContext);
        appendClause(sb, "Themes", themes);
        appendClause(sb, "Similar to", similarTo);
        appendClause(sb, "Mood", moods);
        appendClause(sb, "By authors", authors);
        return sb.toString().trim();
    }

    private static void addAll(Set<String> target, List<String> values) {
        for (String v : values) {
            if (v != null) {
                String t = v.trim();
                if (t.length() >= 2) {
                    target.add(t);
                }
            }
        }
    }

    private static void appendClause(StringBuilder sb, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            sb.append(". ").append(label).append(": ").append(String.join(", ", values));
        }
    }

    public boolean isEmpty() {
        return retrievalTerms().isEmpty();
    }

    /** English stopwords stripped from the deterministic keyword fallback. */
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "with", "without", "for", "to", "of", "in", "on",
            "about", "that", "this", "something", "anything", "book", "books", "novel", "novels",
            "read", "reading", "want", "like", "looking", "me", "my", "i", "im", "is", "are", "be",
            "can", "finish", "really", "some", "give", "recommend", "please", "story", "stories");

    /**
     * Deterministic fallback intent: just the meaningful words of the query as
     * keywords. Used when the LLM is unavailable or returns nothing, so
     * explicit-query search still retrieves relevant books with no AI involved.
     */
    public static QueryIntent keywordFallback(String rawQuery) {
        List<String> keywords = new ArrayList<>();
        if (rawQuery != null) {
            for (String token : rawQuery.toLowerCase(Locale.ROOT).split("\\W+")) {
                if (token.length() >= 3 && !STOPWORDS.contains(token) && !keywords.contains(token)) {
                    keywords.add(token);
                }
            }
        }
        return new QueryIntent(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), keywords, null);
    }
}
