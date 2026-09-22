package com.novi.service;

import com.novi.dto.book.AuthorDto;
import com.novi.dto.book.BookResponse;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.book.GenreDto;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.exception.ResourceNotFoundException;
import com.novi.entity.Genre;
import com.novi.repository.AuthorRepository;
import com.novi.repository.BookRepository;
import com.novi.repository.GenreRepository;
import com.novi.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    /**
     * Self-reference so per-book imports run through the transactional proxy
     * (a plain {@code this.} call would bypass it). Lets each imported book be
     * its own short transaction instead of holding one connection open across
     * the whole batch of external lookups. @Lazy breaks the self-referential
     * bean cycle at construction time.
     */
    @Autowired
    @Lazy
    private BookService self;

    private static final String SOURCE = "open-library";

    /** Book.description is a length-4000 column; keep provider text within it. */
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    /** Genre.name is a length-100 column; skip noisier, longer subject strings. */
    private static final int MAX_GENRE_NAME_LENGTH = 100;
    /**
     * Open Library returns dozens of loosely-curated subjects per work. Keep a
     * handful so genres stay meaningful signal rather than noise.
     */
    private static final int MAX_GENRES_PER_BOOK = 8;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final RatingRepository ratingRepository;
    private final OpenLibraryService openLibraryService;
    private final BookEmbeddingService bookEmbeddingService;
    private final BookThemeTaggingService bookThemeTaggingService;

    /**
     * Search combines whatever is already cached locally with a live lookup
     * against the external metadata provider, importing any new results so
     * future searches (and the library/rating/review features) can reference
     * a real, persisted Book row.
     *
     * <p>Deliberately NOT {@code @Transactional}: the external HTTP lookup and
     * each book's import run outside any single long-lived transaction, so we
     * don't hold one database connection open for the entire batch. Each book
     * is imported and summarized in its own short transaction via {@link #self}.
     */
    public List<BookSummaryResponse> search(String query) {
        List<OpenLibraryService.ExternalBook> externalResults = openLibraryService.search(query, 20);

        List<BookSummaryResponse> summaries = new ArrayList<>(externalResults.size());
        for (OpenLibraryService.ExternalBook external : externalResults) {
            summaries.add(self.importAndSummarize(external));
        }
        return summaries;
    }

    /**
     * Imports a single external result if not already present and maps it to a
     * summary, all within one short transaction. Building the summary here (not
     * in the caller) keeps the lazy author collection accessible while the
     * transaction is still open, since {@code open-in-view} is disabled.
     */
    @Transactional
    public BookSummaryResponse importAndSummarize(OpenLibraryService.ExternalBook external) {
        return toSummary(importIfNeeded(external));
    }

    @Transactional
    public Book importIfNeeded(OpenLibraryService.ExternalBook external) {
        return bookRepository.findByExternalMetadataSourceAndExternalMetadataId(SOURCE, external.externalId())
                .orElseGet(() -> {
                    Book book = Book.builder()
                            .title(external.title())
                            .coverImageUrl(external.coverImageUrl())
                            .isbn(external.isbn())
                            .externalMetadataId(external.externalId())
                            .externalMetadataSource(SOURCE)
                            .build();

                    if (external.publicationDate() != null) {
                        try {
                            book.setPublicationDate(LocalDate.parse(external.publicationDate()));
                        } catch (Exception ignored) {
                            // malformed date from provider - leave null rather than guessing
                        }
                    }

                    for (String name : external.authorNames()) {
                        Author author = authorRepository.findByNameIgnoreCase(name)
                                .orElseGet(() -> authorRepository.save(Author.builder().name(name).build()));
                        book.getAuthors().add(author);
                    }

                    // The search endpoint doesn't return a description or subjects,
                    // so pull the richer "work" document once, at import time. This
                    // is what gives book detail pages a real description and gives
                    // the taste profile / recommender genre signal to work with.
                    openLibraryService.fetchWorkDetails(external.externalId()).ifPresent(details -> {
                        applyDescription(book, details.description());
                        applyGenres(book, details.subjects());
                    });

                    Book saved = bookRepository.save(book);

                    // Best-effort, non-blocking-on-failure enrichment: if the AI
                    // providers aren't configured, these are silent no-ops and the
                    // book is still fully usable for Phase 1 features. Runs after the
                    // description/genres are set so the embedding representation and
                    // theme tagging see the full metadata.
                    bookEmbeddingService.ensureEmbedding(saved);
                    bookThemeTaggingService.ensureThemes(saved);

                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public BookResponse getById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " not found"));
        return toDetail(book);
    }

    @Transactional(readOnly = true)
    public Book getEntityById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<BookSummaryResponse> browse(int page, int size) {
        return bookRepository.findAll(PageRequest.of(page, size)).map(this::toSummary);
    }

    private void applyDescription(Book book, String description) {
        if (description == null || description.isBlank()) {
            return;
        }
        String trimmed = description.length() > MAX_DESCRIPTION_LENGTH
                ? description.substring(0, MAX_DESCRIPTION_LENGTH)
                : description;
        book.setDescription(trimmed);
    }

    private void applyGenres(Book book, List<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        subjects.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank() && name.length() <= MAX_GENRE_NAME_LENGTH)
                .distinct()
                .limit(MAX_GENRES_PER_BOOK)
                .forEach(name -> {
                    Genre genre = genreRepository.findByNameIgnoreCase(name)
                            .orElseGet(() -> genreRepository.save(Genre.builder().name(name).build()));
                    book.getGenres().add(genre);
                });
    }

    private BookSummaryResponse toSummary(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getAuthors().stream().map(Author::getName).toList()
        );
    }

    private BookResponse toDetail(Book book) {
        Double avg = ratingRepository.findAverageRatingForBook(book);
        long count = ratingRepository.countByBook(book);

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getCoverImageUrl(),
                book.getIsbn(),
                book.getPublicationDate(),
                book.getAuthors().stream().map(a -> new AuthorDto(a.getId(), a.getName())).toList(),
                book.getGenres().stream().map(g -> new GenreDto(g.getId(), g.getName())).toList(),
                avg,
                count
        );
    }
}
