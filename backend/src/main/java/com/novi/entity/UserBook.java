package com.novi.entity;

import com.novi.entity.enums.ReadingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single user's personal-library entry for a book: their reading status.
 * A (user, book) pair is unique - a book cannot appear twice in the same library.
 */
@Entity
@Table(name = "user_books", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_books_user_book", columnNames = {"user_id", "book_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

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
