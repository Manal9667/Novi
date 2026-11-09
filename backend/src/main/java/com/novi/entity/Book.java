package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_books_title", columnList = "title"),
        @Index(name = "idx_books_isbn", columnList = "isbn")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(length = 20)
    private String isbn;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "author_id"})
    )
    @Builder.Default
    @BatchSize(size = 50)
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_genres",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "genre_id"})
    )
    @Builder.Default
    @BatchSize(size = 50)
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_themes",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "theme_id"})
    )
    @Builder.Default
    @BatchSize(size = 50)
    private Set<Theme> themes = new HashSet<>();

    /**
     * Semantic embedding of this book (title + authors + genres + themes +
     * description), stored as a JSON float array. It is mirrored into the native
     * pgvector column {@code embedding_vec} for index-backed retrieval (see the
     * pgvector migration). Null until {@code BookEmbeddingService} has processed
     * the book.
     */
    @Column(name = "embedding", columnDefinition = "text")
    private String embedding;

    @Column(name = "embedding_model", length = 50)
    private String embeddingModel;

    @Column(name = "embedding_updated_at")
    private java.time.Instant embeddingUpdatedAt;

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
