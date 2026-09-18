package com.novi.service;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.shelf.ShelfResponse;
import com.novi.entity.*;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.ShelfBookRepository;
import com.novi.repository.ShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelfService {

    private final ShelfRepository shelfRepository;
    private final ShelfBookRepository shelfBookRepository;
    private final BookService bookService;

    @Transactional
    public ShelfResponse create(User user, String name) {
        shelfRepository.findByUserAndNameIgnoreCase(user, name).ifPresent(s -> {
            throw new DuplicateResourceException("You already have a shelf named '" + name + "'");
        });
        Shelf shelf = shelfRepository.save(Shelf.builder().user(user).name(name).build());
        return toResponse(shelf);
    }

    @Transactional
    public void delete(User user, Long shelfId) {
        Shelf shelf = getOwnedShelf(user, shelfId);
        shelfRepository.delete(shelf);
    }

    public List<ShelfResponse> getAll(User user) {
        return shelfRepository.findByUser(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShelfResponse addBook(User user, Long shelfId, Long bookId) {
        Shelf shelf = getOwnedShelf(user, shelfId);
        Book book = bookService.getEntityById(bookId);

        if (shelfBookRepository.existsByShelfAndBook(shelf, book)) {
            throw new DuplicateResourceException("Book is already on this shelf");
        }

        shelfBookRepository.save(ShelfBook.builder().shelf(shelf).book(book).build());
        return toResponse(shelf);
    }

    @Transactional
    public ShelfResponse removeBook(User user, Long shelfId, Long bookId) {
        Shelf shelf = getOwnedShelf(user, shelfId);
        Book book = bookService.getEntityById(bookId);

        ShelfBook shelfBook = shelfBookRepository.findByShelfAndBook(shelf, book)
                .orElseThrow(() -> new ResourceNotFoundException("Book is not on this shelf"));
        shelfBookRepository.delete(shelfBook);

        return toResponse(shelf);
    }

    private Shelf getOwnedShelf(User user, Long shelfId) {
        return shelfRepository.findByIdAndUser(shelfId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf " + shelfId + " not found"));
    }

    private ShelfResponse toResponse(Shelf shelf) {
        List<BookSummaryResponse> books = shelfBookRepository.findByShelf(shelf).stream()
                .map(ShelfBook::getBook)
                .map(b -> new BookSummaryResponse(
                        b.getId(), b.getTitle(), b.getCoverImageUrl(),
                        b.getAuthors().stream().map(Author::getName).toList()))
                .toList();

        return new ShelfResponse(shelf.getId(), shelf.getName(), shelf.getCreatedAt(), books);
    }
}
