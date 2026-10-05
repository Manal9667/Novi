package com.novi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.config.AiProperties;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.repository.BookRepository;
import com.novi.repository.UserBookRepository;
import com.novi.service.RecommendationRerankingService.RankedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The agentic brain behind "Find Your Next Read". Instead of a fixed pipeline,
 * this runs Gemini in a tool-calling loop: the model decides when to search the
 * local catalog, when to reach out to Open Library for books Novi doesn't have
 * yet, when to look at the reader's taste, and finally submits its picks.
 *
 * <p>Grounding: the model can only recommend books it has actually seen via a
 * tool result (tracked in {@code known}); any book id it submits that wasn't
 * surfaced by a tool is dropped. So the agent reasons freely but can never
 * invent a book - every recommendation resolves to a real, persisted row.
 *
 * <p>Fails closed: on any error, exhausted steps, or no LLM, it returns empty
 * and the caller falls back to the deterministic retrieval pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationAgentService {

    private static final int MAX_STEPS = 5;
    private static final int CATALOG_SEARCH_LIMIT = 25;
    private static final int MAX_TOKENS = 1500;

    private final LlmClient llmClient;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final TasteProfileService tasteProfileService;
    private final UserBookRepository userBookRepository;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are Novi, a warm, well-read bookseller helping a reader find their
            next book. Satisfy the reader's request by REASONING and USING TOOLS -
            do not answer from memory alone.

            You have these tools:
            - search_catalog(query): search books Novi already knows. Returns real book ids.
            - search_external(query): bring in matching books from Open Library when
              the catalog lacks what the request needs (e.g. a specific real title,
              a popular/bestselling book, an author). Returns real book ids.
            - get_reader_taste(): the reader's top genres/themes and some of their books,
              to personalize. Use it only as a secondary tie-breaker.
            - submit_recommendations(recommendations): finish by returning your picks.

            Workflow:
            1. Think about what the request really means (genre, mood, theme, popularity,
               "similar to", author, setting, length).
            2. Use search_catalog and/or search_external to gather REAL candidate books.
               Prefer search_external for specific titles/authors or popularity-based
               requests the catalog won't contain.
            3. Optionally call get_reader_taste to break ties.
            4. Call submit_recommendations with up to %d books.

            Rules for submit_recommendations:
            - Only use bookId values that appeared in a tool result. Never invent ids.
            - The request is the PRIMARY signal; personalization is only a tie-breaker.
            - Each pick needs a short, specific reason tied to the REQUEST and an honest
              matchPercent (0-100, not always high). Add a brief potentialDownside or null.
            - Prefer fewer, genuinely-fitting books over padding with irrelevant ones.
            """;

    /**
     * Runs the agent for a natural-language request. Returns the ranked picks,
     * or empty if the agent couldn't complete (caller should fall back).
     */
    public Optional<List<RankedResult>> run(User user, String query) {
        if (!llmClient.isAvailable() || query == null || query.isBlank()) {
            return Optional.empty();
        }

        int limit = aiProperties.getRecommendationCount();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", String.format(SYSTEM_PROMPT, limit)));
        messages.add(message("user", "The reader's request: \"" + query + "\""));

        // Books the agent has actually seen via tool results - the grounding set.
        Map<Long, Book> known = new LinkedHashMap<>();

        for (int step = 0; step < MAX_STEPS; step++) {
            Optional<JsonNode> replyOpt = llmClient.chat(messages, tools(), MAX_TOKENS);
            if (replyOpt.isEmpty()) {
                return Optional.empty();
            }
            JsonNode reply = replyOpt.get();
            JsonNode toolCalls = reply.path("tool_calls");

            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                // Model answered without a tool call: nothing to submit cleanly.
                log.info("Agent[{}]: stopped without submitting after {} step(s)", query, step);
                return Optional.empty();
            }

            // Echo the assistant turn (with its tool_calls) back into the transcript.
            messages.add(objectMapper.convertValue(reply, Map.class));

            for (JsonNode call : toolCalls) {
                String id = call.path("id").asText();
                String name = call.path("function").path("name").asText();
                String argsJson = call.path("function").path("arguments").asText("{}");
                log.info("Agent[{}]: step {} -> {}({})", query, step + 1, name, argsJson);

                if ("submit_recommendations".equals(name)) {
                    List<RankedResult> ranked = parseSubmission(argsJson, known, limit);
                    log.info("Agent[{}]: submitted {} grounded recommendation(s)", query, ranked.size());
                    return ranked.isEmpty() ? Optional.empty() : Optional.of(ranked);
                }

                String result = executeTool(user, name, argsJson, known);
                messages.add(toolResult(id, result));
            }
        }

        log.info("Agent[{}]: reached step limit without submitting", query);
        return Optional.empty();
    }

    // ---- Tool execution ----

    private String executeTool(User user, String name, String argsJson, Map<Long, Book> known) {
        try {
            JsonNode args = objectMapper.readTree(argsJson == null || argsJson.isBlank() ? "{}" : argsJson);
            return switch (name) {
                case "search_catalog" -> searchCatalog(args.path("query").asText(""), known);
                case "search_external" -> searchExternal(args.path("query").asText(""), known);
                case "get_reader_taste" -> readerTaste(user);
                default -> "{\"error\":\"unknown tool\"}";
            };
        } catch (Exception e) {
            log.warn("Agent tool '{}' failed: {}", name, e.getMessage());
            return "{\"error\":\"tool failed\"}";
        }
    }

    private String searchCatalog(String query, Map<Long, Book> known) {
        if (query == null || query.isBlank()) {
            return "[]";
        }
        String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        List<Book> books = bookRepository.searchByTerm(pattern, PageRequest.of(0, CATALOG_SEARCH_LIMIT));
        return booksToJson(books, known);
    }

    private String searchExternal(String query, Map<Long, Book> known) {
        if (query == null || query.isBlank()) {
            return "[]";
        }
        // Imports matching books from Open Library (title/description/genres), so
        // they become real, persisted rows the agent can then recommend.
        List<BookSummaryResponse> imported = bookService.search(query);
        List<Long> ids = imported.stream().map(BookSummaryResponse::id).toList();
        List<Book> books = bookRepository.findAllById(ids);
        return booksToJson(books, known);
    }

    private String readerTaste(User user) {
        List<String> genres = tasteProfileService.getGenreAffinities(user).stream()
                .limit(6).map(a -> a.getGenre().getName()).toList();
        List<String> themes = tasteProfileService.getThemeAffinities(user).stream()
                .limit(6).map(a -> a.getTheme().getName()).toList();
        List<String> library = userBookRepository.findByUser(user).stream()
                .limit(10).map(ub -> ub.getBook().getTitle()).toList();

        Map<String, Object> taste = new LinkedHashMap<>();
        taste.put("topGenres", genres);
        taste.put("topThemes", themes);
        taste.put("booksInLibrary", library);
        return writeJson(taste);
    }

    private String booksToJson(List<Book> books, Map<Long, Book> known) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Book b : books) {
            known.put(b.getId(), b);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bookId", b.getId());
            m.put("title", b.getTitle());
            m.put("authors", b.getAuthors().stream().map(Author::getName).toList());
            m.put("genres", b.getGenres().stream().map(Genre::getName).limit(5).toList());
            String desc = b.getDescription();
            if (desc != null && !desc.isBlank()) {
                m.put("description", desc.length() > 180 ? desc.substring(0, 180) + "..." : desc);
            }
            out.add(m);
        }
        return writeJson(out);
    }

    // ---- Final submission parsing (grounded) ----

    private List<RankedResult> parseSubmission(String argsJson, Map<Long, Book> known, int limit) {
        List<RankedResult> results = new ArrayList<>();
        try {
            JsonNode args = objectMapper.readTree(argsJson == null || argsJson.isBlank() ? "{}" : argsJson);
            JsonNode recs = args.path("recommendations");
            if (!recs.isArray()) {
                return results;
            }
            for (JsonNode node : recs) {
                if (results.size() >= limit) break;
                long bookId = node.path("bookId").asLong(-1);
                Book book = known.get(bookId); // grounding: must have been seen via a tool
                if (book == null) continue;

                List<String> reasons = new ArrayList<>();
                node.path("reasons").forEach(r -> reasons.add(r.asText()));
                if (reasons.isEmpty() && node.hasNonNull("reason")) {
                    reasons.add(node.path("reason").asText());
                }
                String downside = node.path("potentialDownside").isNull() ? null
                        : node.path("potentialDownside").asText(null);
                double pct = Math.max(0, Math.min(100, node.path("matchPercent").asDouble(0)));

                results.add(new RankedResult(book, pct / 100.0, reasons, downside));
            }
        } catch (Exception e) {
            log.warn("Agent submission parse failed: {}", e.getMessage());
        }
        return results;
    }

    // ---- Message / tool-spec helpers ----

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private Map<String, Object> toolResult(String toolCallId, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "tool");
        m.put("tool_call_id", toolCallId);
        m.put("content", content);
        return m;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<Map<String, Object>> tools() {
        return List.of(
                functionTool("search_catalog",
                        "Search books Novi already has by title, author, genre, theme or description. Returns real bookIds.",
                        Map.of("query", stringParam("What to search for, e.g. 'historical fiction China' or an author name"))),
                functionTool("search_external",
                        "Fetch matching books from Open Library and import them so they become recommendable. Use for specific real titles, authors, or popularity-based requests the catalog may lack.",
                        Map.of("query", stringParam("A title, author, or descriptive search, e.g. 'The Love Hypothesis' or 'bestselling romance'"))),
                functionTool("get_reader_taste",
                        "Get the reader's top genres/themes and some books in their library, to personalize as a tie-breaker.",
                        Map.of()),
                submitTool());
    }

    private Map<String, Object> functionTool(String name, String description, Map<String, ?> properties) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        params.put("required", new ArrayList<>(properties.keySet()));

        Map<String, Object> fn = new LinkedHashMap<>();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", params);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    private Map<String, Object> submitTool() {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("type", "object");
        rec.put("properties", Map.of(
                "bookId", Map.of("type", "integer", "description", "A bookId from a tool result"),
                "matchPercent", Map.of("type", "integer", "description", "0-100 fit for the request"),
                "reasons", Map.of("type", "array", "items", Map.of("type", "string"),
                        "description", "Short reasons tied to the request"),
                "potentialDownside", Map.of("type", "string", "description", "An honest caveat, or omit")));
        rec.put("required", List.of("bookId", "matchPercent", "reasons"));

        Map<String, Object> arrayParam = new LinkedHashMap<>();
        arrayParam.put("type", "array");
        arrayParam.put("items", rec);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("recommendations", arrayParam);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", List.of("recommendations"));

        Map<String, Object> fn = new LinkedHashMap<>();
        fn.put("name", "submit_recommendations");
        fn.put("description", "Submit the final list of recommended books (grounded in tool results).");
        fn.put("parameters", params);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    private Map<String, Object> stringParam(String description) {
        return Map.of("type", "string", "description", description);
    }
}
