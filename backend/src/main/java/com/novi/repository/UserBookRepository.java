package com.novi.repository;

import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.entity.enums.ReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    Optional<UserBook> findByUserAndBook(User user, Book book);
    boolean existsByUserAndBook(User user, Book book);

    // Unpaged variants used by the recommendation pipeline, which needs to
    // consider the user's entire library rather than a single page.
    List<UserBook> findByUser(User user);
    List<UserBook> findByUserAndStatus(User user, ReadingStatus status);

    // Paged variants backing the /library endpoint. The book is fetched
    // eagerly (to-one join, no row multiplication); its authors/genres are
    // batch-loaded via @BatchSize on the collections, avoiding N+1.
    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserAndStatus(User user, ReadingStatus status, Pageable pageable);
}
