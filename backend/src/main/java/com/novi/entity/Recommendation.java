package com.novi.entity;

import com.novi.entity.enums.RecommendationSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    /** JSON array of short reason strings, e.g. ["You rated Dune 5 stars", ...] */
    @Column(nullable = false, columnDefinition = "text")
    private String reasons;

    @Column(name = "potential_downside", columnDefinition = "text")
    private String potentialDownside;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationSource source;

    @Column(name = "query_text", length = 500)
    private String queryText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
