package com.novi.repository;

import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.UserBook;
import com.novi.entity.enums.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    Optional<UserBook> findByUserAndBook(User user, Book book);
    boolean existsByUserAndBook(User user, Book book);
    List<UserBook> findByUser(User user);
    List<UserBook> findByUserAndStatus(User user, ReadingStatus status);
}
