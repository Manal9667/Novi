package com.novi.repository;

import com.novi.entity.Book;
import com.novi.entity.Review;
import com.novi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookOrderByCreatedAtDesc(Book book);
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    Optional<Review> findByUserAndBook(User user, Book book);
}
