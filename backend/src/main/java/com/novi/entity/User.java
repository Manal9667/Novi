package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "bio", length = 500)
    private String bio;

    /**
     * A weighted average of embeddings for books this user has responded
     * positively to, recomputed by {@code TasteProfileService}. Stored as a
     * JSON float array for the same reason as {@code Book.embedding}.
     */
    @Column(name = "taste_vector", columnDefinition = "text")
    private String tasteVector;

    @Column(name = "taste_vector_updated_at")
    private Instant tasteVectorUpdatedAt;

    /**
     * True when a taste-affecting signal (rating, library change, review,
     * recommendation feedback) has occurred since the taste profile was last
     * recomputed. Read paths recompute only when this is set, instead of on
     * every request. New users start stale so their profile is built on first read.
     */
    @Column(name = "taste_profile_stale", nullable = false)
    @Builder.Default
    private boolean tasteProfileStale = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
