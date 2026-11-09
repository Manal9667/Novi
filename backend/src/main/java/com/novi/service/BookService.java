package com.novi.service;

import com.novi.dto.book.AuthorDto;
import com.novi.dto.book.BookResponse;
import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.book.GenreDto;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to the seeded book catalog. The catalog is populated by the
 * data-seeding process (see {@code CatalogSeeder}), not imported on demand, so
 * this service is deliberately read-only.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public BookResponse getById(Long id) {
        return toDetail(getEntityById(id));
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

    private BookSummaryResponse toSummary(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getAuthors().stream().map(Author::getName).toList()
        );
    }

    private BookResponse toDetail(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getCoverImageUrl(),
                book.getIsbn(),
                book.getPublicationDate(),
                book.getAuthors().stream().map(a -> new AuthorDto(a.getId(), a.getName())).toList(),
                book.getGenres().stream().map(g -> new GenreDto(g.getId(), g.getName())).toList(),
                book.getThemes().stream().map(com.novi.entity.Theme::getName).toList()
        );
    }
}
