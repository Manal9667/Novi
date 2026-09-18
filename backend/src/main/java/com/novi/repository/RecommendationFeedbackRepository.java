package com.novi.repository;

import com.novi.entity.Recommendation;
import com.novi.entity.RecommendationFeedback;
import com.novi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {
    Optional<RecommendationFeedback> findByRecommendation(Recommendation recommendation);
    List<RecommendationFeedback> findByUser(User user);
}
