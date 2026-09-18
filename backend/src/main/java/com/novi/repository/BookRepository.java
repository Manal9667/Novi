package com.novi.repository;

import com.novi.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByExternalMetadataSourceAndExternalMetadataId(String source, String externalId);
    Optional<Book> findByIsbn(String isbn);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    java.util.List<Book> findByEmbeddingIsNotNull();
    java.util.List<Book> findByEmbeddingIsNull();
}
