package com.novi.repository;

import com.novi.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByNameIgnoreCase(String name);
}
