package com.novi.repository;

import com.novi.entity.Book;
import com.novi.entity.Rating;
import com.novi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserAndBook(User user, Book book);
    void deleteByUserAndBook(User user, Book book);

    @org.springframework.data.jpa.repository.Query(
            "select avg(r.stars) from Rating r where r.book = :book")
    Double findAverageRatingForBook(Book book);

    long countByBook(Book book);
}
