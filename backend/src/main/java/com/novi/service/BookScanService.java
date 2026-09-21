package com.novi.service;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.library.UserBookResponse;
import com.novi.dto.scan.*;
import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.ScanCandidate;
import com.novi.entity.ScanSession;
import com.novi.entity.User;
import com.novi.entity.enums.ReadingStatus;
import com.novi.entity.enums.ScanCandidateStatus;
import com.novi.entity.enums.ScanSessionStatus;
import com.novi.entity.enums.ScanType;
import com.novi.exception.BadRequestException;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.exception.ServiceUnavailableException;
import com.novi.repository.ScanSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3 orchestration: turn an uploaded photo into a reviewable set of
 * detected books, then apply the user's confirmations to their library.
 *
 * The pipeline is deliberately two-step - scan (persist candidates) then
 * confirm - so that nothing is ever silently added: every book waits for an
 * explicit decision, and low-confidence detections are surfaced with their
 * score rather than guessed into the library. This also connects the scanner
 * to Phase 2: confirmed books flow through {@link LibraryService}, enriching
 * the reading history that the recommendation engine learns from.
 */
@Service
@RequiredArgsConstructor
public class BookScanService {

    private final VisionService visionService;
    private final BookMatchingService bookMatchingService;
    private final LibraryService libraryService;
    private final BookService bookService;
    private final ScanSessionRepository scanSessionRepository;

    @Transactional
    public ScanResultResponse scanSingleBook(User user, byte[] imageBytes, String mediaType) {
        requireVision();
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

        ScanSession session = newSession(user, ScanType.SINGLE_BOOK);

        visionService.detectSingleBook(base64, mediaType).ifPresent(detected -> {
            BookMatchingService.MatchResult match = bookMatchingService.match(detected.title(), detected.author());
            session.getCandidates().add(buildCandidate(session, detected, match));
        });

        session.setDetectedCount(session.getCandidates().size());
        return persistAndConvert(session);
    }

    @Transactional
    public ScanResultResponse scanShelf(User user, byte[] imageBytes, String mediaType) {
        requireVision();
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

        ScanSession session = newSession(user, ScanType.SHELF);

        List<VisionService.DetectedBook> detectedBooks = visionService.detectShelfBooks(base64, mediaType);

        // De-duplicate a shelf so the same book read from two angles, or two
        // near-identical spine reads, don't show up twice. Keyed by matched
        // book id when we have one, else by the normalized detected title;
        // we keep the highest-confidence candidate for each key.
        Map<String, ScanCandidate> byKey = new LinkedHashMap<>();
        for (VisionService.DetectedBook detected : detectedBooks) {
            BookMatchingService.MatchResult match = bookMatchingService.match(detected.title(), detected.author());
            ScanCandidate candidate = buildCandidate(session, detected, match);

            String key = match.book() != null
                    ? "book:" + match.book().getId()
                    : "title:" + detected.title().toLowerCase().trim();

            ScanCandidate existing = byKey.get(key);
            if (existing == null || candidate.getConfidence() > existing.getConfidence()) {
                byKey.put(key, candidate);
            }
        }

        session.getCandidates().addAll(byKey.values());
        session.setDetectedCount(session.getCandidates().size());
        return persistAndConvert(session);
    }

    @Transactional(readOnly = true)
    public ScanResultResponse getSession(User user, Long sessionId) {
        return toResponse(loadSession(user, sessionId));
    }

    @Transactional
    public ConfirmScanResponse confirm(User user, Long sessionId, ConfirmScanRequest request) {
        ScanSession session = loadSession(user, sessionId);

        Map<Long, ScanCandidate> candidatesById = new LinkedHashMap<>();
        for (ScanCandidate c : session.getCandidates()) {
            candidatesById.put(c.getId(), c);
        }

        int added = 0;
        int skipped = 0;
        int alreadyInLibrary = 0;
        List<UserBookResponse> addedBooks = new ArrayList<>();

        for (CandidateDecision decision : request.decisions()) {
            ScanCandidate candidate = candidatesById.get(decision.candidateId());
            if (candidate == null) {
                throw new ResourceNotFoundException("Scan candidate " + decision.candidateId() + " not found in this scan");
            }

            if (!decision.confirm()) {
                candidate.setStatus(ScanCandidateStatus.SKIPPED);
                skipped++;
                continue;
            }

            Long bookId = resolveBookId(candidate, decision);
            ReadingStatus status = decision.status() != null ? decision.status() : ReadingStatus.WANT_TO_READ;
            try {
                UserBookResponse response = libraryService.addBook(user, bookId, status);
                addedBooks.add(response);
                added++;
            } catch (DuplicateResourceException alreadyThere) {
                // Confirming a book already in the library is not an error - the
                // scan just overlapped with what the reader already tracked.
                alreadyInLibrary++;
            }
            candidate.setStatus(ScanCandidateStatus.CONFIRMED);
        }

        session.setStatus(ScanSessionStatus.COMPLETED);
        scanSessionRepository.save(session);

        return new ConfirmScanResponse(added, skipped, alreadyInLibrary, addedBooks);
    }

    // ------------------------------------------------------------------

    private void requireVision() {
        if (!visionService.isAvailable()) {
            throw new ServiceUnavailableException(
                    "The book scanner requires a vision model to be configured. Set ANTHROPIC_API_KEY to enable it.");
        }
    }

    private ScanSession newSession(User user, ScanType type) {
        return ScanSession.builder()
                .user(user)
                .type(type)
                .status(ScanSessionStatus.AWAITING_CONFIRMATION)
                .detectedCount(0)
                .build();
    }

    private ScanCandidate buildCandidate(ScanSession session, VisionService.DetectedBook detected,
                                         BookMatchingService.MatchResult match) {
        double combined = round(detected.confidence() * match.confidence());
        return ScanCandidate.builder()
                .session(session)
                .detectedTitle(detected.title())
                .detectedAuthor(detected.author())
                .visionConfidence(detected.confidence())
                .matchConfidence(match.confidence())
                .confidence(combined)
                .matchedBook(match.book())
                .status(ScanCandidateStatus.PENDING)
                .build();
    }

    private Long resolveBookId(ScanCandidate candidate, CandidateDecision decision) {
        if (decision.overrideBookId() != null) {
            // "Choose Another": validate the substitute exists before using it.
            Book override = bookService.getEntityById(decision.overrideBookId());
            return override.getId();
        }
        if (candidate.getMatchedBook() == null) {
            throw new BadRequestException(
                    "Candidate " + candidate.getId() + " has no matched book; provide overrideBookId to confirm it");
        }
        return candidate.getMatchedBook().getId();
    }

    private ScanSession loadSession(User user, Long sessionId) {
        return scanSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Scan " + sessionId + " not found"));
    }

    private ScanResultResponse persistAndConvert(ScanSession session) {
        ScanSession saved = scanSessionRepository.save(session);
        return toResponse(saved);
    }

    private ScanResultResponse toResponse(ScanSession session) {
        List<ScanCandidateResponse> candidates = session.getCandidates().stream()
                .map(this::toCandidateResponse)
                .toList();
        return new ScanResultResponse(
                session.getId(),
                session.getType(),
                session.getDetectedCount(),
                session.getStatus(),
                candidates);
    }

    private ScanCandidateResponse toCandidateResponse(ScanCandidate candidate) {
        Book book = candidate.getMatchedBook();
        BookSummaryResponse summary = book == null ? null : new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getAuthors().stream().map(Author::getName).toList());
        return new ScanCandidateResponse(
                candidate.getId(),
                candidate.getDetectedTitle(),
                candidate.getDetectedAuthor(),
                candidate.getConfidence(),
                summary,
                candidate.getStatus());
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
