package com.novi.service;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.common.PageResponse;
import com.novi.dto.library.UserBookResponse;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.entity.enums.HistoryEventType;
import com.novi.entity.enums.ReadingStatus;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserBookRepository userBookRepository;
    private final BookService bookService;
    private final ReadingHistoryService readingHistoryService;

    @Transactional
    public UserBookResponse addBook(User user, Long bookId, ReadingStatus requestedStatus) {
        Book book = bookService.getEntityById(bookId);

        if (userBookRepository.existsByUserAndBook(user, book)) {
            throw new DuplicateResourceException("Book is already in your library");
        }

        ReadingStatus status = requestedStatus != null ? requestedStatus : ReadingStatus.WANT_TO_READ;

        UserBook userBook = UserBook.builder()
                .user(user)
                .book(book)
                .status(status)
                .startedAt(status == ReadingStatus.CURRENTLY_READING ? Instant.now() : null)
                .finishedAt(status == ReadingStatus.READ ? Instant.now() : null)
                .build();

        userBook = userBookRepository.save(userBook);
        readingHistoryService.record(user, book, HistoryEventType.ADDED_TO_LIBRARY);

        return toResponse(userBook);
    }

    @Transactional
    public void removeBook(User user, Long bookId) {
        Book book = bookService.getEntityById(bookId);
        UserBook userBook = userBookRepository.findByUserAndBook(user, book)
                .orElseThrow(() -> new ResourceNotFoundException("Book is not in your library"));

        userBookRepository.delete(userBook);
        readingHistoryService.record(user, book, HistoryEventType.REMOVED_FROM_LIBRARY);
    }

    @Transactional
    public UserBookResponse updateStatus(User user, Long bookId, ReadingStatus newStatus) {
        Book book = bookService.getEntityById(bookId);
        UserBook userBook = userBookRepository.findByUserAndBook(user, book)
                .orElseThrow(() -> new ResourceNotFoundException("Book is not in your library"));

        userBook.setStatus(newStatus);

        HistoryEventType eventType;
        switch (newStatus) {
            case CURRENTLY_READING -> {
                if (userBook.getStartedAt() == null) userBook.setStartedAt(Instant.now());
                eventType = HistoryEventType.STARTED_READING;
            }
            case READ -> {
                userBook.setFinishedAt(Instant.now());
                eventType = HistoryEventType.FINISHED_READING;
            }
            case DNF -> eventType = HistoryEventType.MARKED_DNF;
            default -> eventType = HistoryEventType.STATUS_CHANGED;
        }

        userBook = userBookRepository.save(userBook);
        readingHistoryService.record(user, book, eventType);

        return toResponse(userBook);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserBookResponse> getLibrary(User user, ReadingStatus filter, Pageable pageable) {
        Page<UserBook> books = filter != null
                ? userBookRepository.findByUserAndStatus(user, filter, pageable)
                : userBookRepository.findByUser(user, pageable);

        return PageResponse.from(books.map(this::toResponse));
    }

    private UserBookResponse toResponse(UserBook ub) {
        Book book = ub.getBook();
        BookSummaryResponse summary = new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getAuthors().stream().map(Author::getName).toList()
        );
        return new UserBookResponse(
                ub.getId(),
                summary,
                ub.getStatus(),
                ub.getStartedAt(),
                ub.getFinishedAt(),
                ub.getCreatedAt()
        );
    }
}
