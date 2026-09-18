package com.novi.entity;

import com.novi.entity.enums.FeedbackType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "recommendation_feedback", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recommendation_feedback", columnNames = "recommendation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private FeedbackType feedbackType;

    @Column(length = 50)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
