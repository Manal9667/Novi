package com.novi.entity;

import com.novi.entity.enums.HistoryEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An append-only log of meaningful reading events for a user.
 * This becomes a key input to the Phase 2 AI recommendation engine,
 * so events are never overwritten or fabricated - only appended when
 * a real user action occurs.
 */
@Entity
@Table(name = "reading_history", indexes = {
        @Index(name = "idx_reading_history_user", columnList = "user_id"),
        @Index(name = "idx_reading_history_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingHistoryEvent {

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
    @Column(nullable = false, length = 30)
    private HistoryEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }
}
