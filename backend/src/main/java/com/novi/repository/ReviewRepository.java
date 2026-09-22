package com.novi.repository;

import com.novi.entity.Book;
import com.novi.entity.Review;
import com.novi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Fetch the author and book up front so mapping to ReviewResponse (which
    // reads the reviewer's username/displayName) doesn't trigger a query per row.
    @EntityGraph(attributePaths = {"user", "book"})
    Page<Review> findByBook(Book book, Pageable pageable);

    List<Review> findByUserOrderByCreatedAtDesc(User user);
    Optional<Review> findByUserAndBook(User user, Book book);

    long countByUser(User user);
}
