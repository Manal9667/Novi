package com.novi.repository;

import com.novi.entity.Recommendation;
import com.novi.entity.Book;
import com.novi.entity.User;
import com.novi.entity.enums.RecommendationSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserOrderByCreatedAtDesc(User user);
    Optional<Recommendation> findFirstByUserAndBookAndSourceAndQueryTextIsNullOrderByCreatedAtDesc(
            User user, Book book, RecommendationSource source);
    Optional<Recommendation> findFirstByUserAndBookAndSourceAndQueryTextOrderByCreatedAtDesc(
            User user, Book book, RecommendationSource source, String queryText);
}
