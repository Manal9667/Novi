package com.novi.repository;

import com.novi.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByNameIgnoreCase(String name);
}
