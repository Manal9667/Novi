package com.novi.repository;

import com.novi.entity.Shelf;
import com.novi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    Page<Shelf> findByUser(User user, Pageable pageable);
    Optional<Shelf> findByUserAndNameIgnoreCase(User user, String name);
    Optional<Shelf> findByIdAndUser(Long id, User user);
}
