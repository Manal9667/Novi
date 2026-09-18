package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

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

    /** id of the record in the external metadata provider (e.g. Open Library work key) */
    @Column(name = "external_metadata_id", length = 100)
    private String externalMetadataId;

    /** which provider externalMetadataId came from, so providers can be swapped later */
    @Column(name = "external_metadata_source", length = 50)
    private String externalMetadataSource;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "author_id"})
    )
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_genres",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "genre_id"})
    )
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_themes",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "theme_id"})
    )
    @Builder.Default
    private Set<Theme> themes = new HashSet<>();

    /**
     * Semantic embedding of this book (title + description + genres/authors),
     * stored as a JSON float array rather than a native pgvector column - see
     * V2__ai_recommendations.sql for why. Null until {@code BookEmbeddingService}
     * has processed the book.
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
