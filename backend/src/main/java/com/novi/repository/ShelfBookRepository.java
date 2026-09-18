package com.novi.repository;

import com.novi.entity.Book;
import com.novi.entity.Shelf;
import com.novi.entity.ShelfBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfBookRepository extends JpaRepository<ShelfBook, Long> {
    List<ShelfBook> findByShelf(Shelf shelf);
    Optional<ShelfBook> findByShelfAndBook(Shelf shelf, Book book);
    boolean existsByShelfAndBook(Shelf shelf, Book book);
}
