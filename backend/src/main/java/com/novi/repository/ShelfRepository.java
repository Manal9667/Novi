package com.novi.repository;

import com.novi.entity.Shelf;
import com.novi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    List<Shelf> findByUser(User user);
    Optional<Shelf> findByUserAndNameIgnoreCase(User user, String name);
    Optional<Shelf> findByIdAndUser(Long id, User user);
}
