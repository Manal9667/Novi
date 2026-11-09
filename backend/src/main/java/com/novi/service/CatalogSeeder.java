package com.novi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.Theme;
import com.novi.repository.AuthorRepository;
import com.novi.repository.BookRepository;
import com.novi.repository.GenreRepository;
import com.novi.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Loads the curated book catalog from {@code classpath:data/books.json} on
 * startup. The process is idempotent: a book already present (matched by ISBN,
 * or title + first author) is skipped, so restarting never duplicates rows and
 * new entries added to the JSON are picked up on the next boot.
 *
 * <p>After seeding, it triggers a one-time embedding backfill for any book that
 * doesn't yet have an embedding. Existing embeddings are never regenerated, so
 * the (paid) embedding calls happen once rather than on every startup. Without
 * an embedding provider configured the backfill is a no-op and retrieval falls
 * back to the lexical path.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogSeeder implements ApplicationRunner {

    private static final String COVER_URL_TEMPLATE =
            "https://covers.openlibrary.org/b/isbn/%s-L.jpg?default=false";

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ThemeRepository themeRepository;
    private final BookEmbeddingService bookEmbeddingService;
    private final ObjectMapper objectMapper;

    /**
     * Self-reference so {@link #seedCatalog()} runs through the transactional
     * proxy. A plain {@code this.seedCatalog()} call would bypass it, committing
     * each author/genre/theme in its own transaction and leaving them detached
     * when the book's cascade tries to re-persist them. @Lazy breaks the
     * self-referential bean cycle at construction time.
     */
    @Autowired
    @Lazy
    private CatalogSeeder self;

    @Value("${novi.catalog.seed-on-startup:true}")
    private boolean seedOnStartup;

    /** Shape of one entry in books.json. */
    public record SeedBook(
            String title,
            List<String> authors,
            List<String> genres,
            List<String> themes,
            Integer publicationYear,
            String isbn,
            String description) {}

    @Override
    public void run(ApplicationArguments args) {
        if (!seedOnStartup) {
            log.info("Catalog seeding disabled (novi.catalog.seed-on-startup=false)");
            return;
        }
        int inserted = self.seedCatalog();
        log.info("Catalog seeding complete: {} new book(s) inserted, {} total in catalog",
                inserted, bookRepository.count());

        int embedded = bookEmbeddingService.backfillMissingEmbeddings(1000);
        if (embedded > 0) {
            log.info("Generated embeddings for {} book(s)", embedded);
        }
    }

    @Transactional
    public int seedCatalog() {
        SeedBook[] seeds = load();
        if (seeds == null) {
            return 0;
        }
        int inserted = 0;
        for (SeedBook seed : seeds) {
            if (seed.title() == null || seed.title().isBlank()) {
                continue;
            }
            if (alreadyPresent(seed)) {
                continue;
            }
            bookRepository.save(toBook(seed));
            inserted++;
        }
        return inserted;
    }

    private SeedBook[] load() {
        ClassPathResource resource = new ClassPathResource("data/books.json");
        if (!resource.exists()) {
            log.warn("Seed file data/books.json not found - catalog will be empty");
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, SeedBook[].class);
        } catch (Exception e) {
            log.error("Failed to read seed catalog data/books.json: {}", e.getMessage());
            return null;
        }
    }

    private boolean alreadyPresent(SeedBook seed) {
        if (seed.isbn() != null && !seed.isbn().isBlank()
                && bookRepository.findByIsbn(seed.isbn()).isPresent()) {
            return true;
        }
        // Fall back to title match so books seeded without an ISBN still dedupe.
        return !bookRepository.findByTitleIgnoreCase(seed.title()).isEmpty();
    }

    private Book toBook(SeedBook seed) {
        Book book = Book.builder()
                .title(seed.title().trim())
                .description(seed.description())
                .isbn(blankToNull(seed.isbn()))
                .coverImageUrl(coverUrl(seed.isbn()))
                .publicationDate(seed.publicationYear() != null
                        ? LocalDate.of(seed.publicationYear(), 1, 1) : null)
                .build();

        if (seed.authors() != null) {
            for (String name : seed.authors()) {
                if (name != null && !name.isBlank()) {
                    book.getAuthors().add(findOrCreateAuthor(name.trim()));
                }
            }
        }
        if (seed.genres() != null) {
            for (String name : seed.genres()) {
                if (name != null && !name.isBlank()) {
                    book.getGenres().add(findOrCreateGenre(name.trim()));
                }
            }
        }
        if (seed.themes() != null) {
            for (String name : seed.themes()) {
                if (name != null && !name.isBlank()) {
                    book.getThemes().add(findOrCreateTheme(name.trim()));
                }
            }
        }
        return book;
    }

    private Author findOrCreateAuthor(String name) {
        return authorRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> authorRepository.save(Author.builder().name(name).build()));
    }

    private Genre findOrCreateGenre(String name) {
        return genreRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> genreRepository.save(Genre.builder().name(name).build()));
    }

    private Theme findOrCreateTheme(String name) {
        return themeRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> themeRepository.save(Theme.builder().name(name).build()));
    }

    private static String coverUrl(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        // default=false makes Open Library return 404 (not a blank placeholder)
        // for an unknown ISBN, so the UI cleanly falls back to its spine design.
        return String.format(COVER_URL_TEMPLATE, isbn.trim());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
