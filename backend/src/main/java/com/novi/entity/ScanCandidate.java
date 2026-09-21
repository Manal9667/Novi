package com.novi.entity;

import com.novi.entity.enums.ScanCandidateStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single book detected within a {@link ScanSession}. Holds both what the
 * vision model read off the spine/cover ({@code detectedTitle}/{@code
 * detectedAuthor}) and, if we could resolve it against the metadata provider,
 * the {@link Book} it was matched to plus a confidence score. Uncertain
 * matches are surfaced to the user rather than added automatically.
 */
@Entity
@Table(name = "scan_candidates", indexes = {
        @Index(name = "idx_scan_candidates_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ScanSession session;

    @Column(name = "detected_title", length = 500)
    private String detectedTitle;

    @Column(name = "detected_author", length = 300)
    private String detectedAuthor;

    /** The vision model's own confidence it read the text correctly, 0.0-1.0. */
    @Column(name = "vision_confidence", nullable = false)
    private double visionConfidence;

    /** How well the detected text matched a real catalog book, 0.0-1.0. */
    @Column(name = "match_confidence", nullable = false)
    private double matchConfidence;

    /** Combined confidence actually shown to the user, 0.0-1.0. */
    @Column(nullable = false)
    private double confidence;

    /** The resolved catalog book, or null if nothing could be matched. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_book_id")
    private Book matchedBook;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanCandidateStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
