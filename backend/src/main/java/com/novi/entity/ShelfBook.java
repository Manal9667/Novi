package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "shelf_books", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shelf_books_shelf_book", columnNames = {"shelf_id", "book_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelfBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @PrePersist
    void onCreate() {
        this.addedAt = Instant.now();
    }
}
