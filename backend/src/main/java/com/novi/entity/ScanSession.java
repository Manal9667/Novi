package com.novi.entity;

import com.novi.entity.enums.ScanSessionStatus;
import com.novi.entity.enums.ScanType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One computer-vision scan a user performed - either a single physical book or
 * a whole bookshelf. The detected books are stored as {@link ScanCandidate}
 * rows so the two-step "scan, then confirm" flow can reference exactly what was
 * detected, and so nothing is added to a library without an explicit decision.
 */
@Entity
@Table(name = "scan_sessions", indexes = {
        @Index(name = "idx_scan_sessions_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScanSessionStatus status;

    /** How many distinct books the vision step detected in the image. */
    @Column(name = "detected_count", nullable = false)
    private int detectedCount;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<ScanCandidate> candidates = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
