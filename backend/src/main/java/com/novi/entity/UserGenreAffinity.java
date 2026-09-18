package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_genre_affinity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_genre_affinity", columnNames = {"user_id", "genre_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGenreAffinity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    /** Normalized affinity score, roughly 0.0 (no interest) to 1.0 (strong interest). */
    @Column(nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
