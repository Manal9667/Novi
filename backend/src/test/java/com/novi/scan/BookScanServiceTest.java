package com.novi.scan;

import com.novi.dto.book.BookSummaryResponse;
import com.novi.dto.library.UserBookResponse;
import com.novi.dto.scan.CandidateDecision;
import com.novi.dto.scan.ConfirmScanRequest;
import com.novi.dto.scan.ConfirmScanResponse;
import com.novi.dto.scan.ScanResultResponse;
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
import com.novi.exception.ServiceUnavailableException;
import com.novi.repository.ScanSessionRepository;
import com.novi.service.BookMatchingService;
import com.novi.service.BookScanService;
import com.novi.service.BookService;
import com.novi.service.LibraryService;
import com.novi.service.VisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookScanServiceTest {

    @Mock private VisionService visionService;
    @Mock private BookMatchingService bookMatchingService;
    @Mock private LibraryService libraryService;
    @Mock private BookService bookService;
    @Mock private ScanSessionRepository scanSessionRepository;

    @InjectMocks
    private BookScanService bookScanService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("reader").displayName("Reader").build();
    }

    private Book book(long id, String title) {
        return Book.builder().id(id).title(title).build();
    }

    @Test
    void scanSingleBook_whenVisionUnavailable_throwsServiceUnavailable() {
        when(visionService.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> bookScanService.scanSingleBook(user, new byte[]{1, 2, 3}, "image/jpeg"))
                .isInstanceOf(ServiceUnavailableException.class);

        verify(visionService, never()).detectSingleBook(anyString(), anyString());
        verifyNoInteractions(libraryService);
    }

    @Test
    void scanSingleBook_detectsAndMatches_butNeverAddsToLibrary() {
        Book dune = book(10L, "Dune");
        when(visionService.isAvailable()).thenReturn(true);
        when(visionService.detectSingleBook(anyString(), eq("image/jpeg")))
                .thenReturn(Optional.of(new VisionService.DetectedBook("Dune", "Frank Herbert", 0.9)));
        when(bookMatchingService.match("Dune", "Frank Herbert"))
                .thenReturn(new BookMatchingService.MatchResult(dune, 0.8));
        when(scanSessionRepository.save(any(ScanSession.class))).thenAnswer(inv -> {
            ScanSession s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });

        ScanResultResponse result = bookScanService.scanSingleBook(user, new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(result.type()).isEqualTo(ScanType.SINGLE_BOOK);
        assertThat(result.status()).isEqualTo(ScanSessionStatus.AWAITING_CONFIRMATION);
        assertThat(result.detectedCount()).isEqualTo(1);
        assertThat(result.candidates()).hasSize(1);
        var candidate = result.candidates().get(0);
        assertThat(candidate.detectedTitle()).isEqualTo("Dune");
        assertThat(candidate.matchedBook()).isNotNull();
        assertThat(candidate.status()).isEqualTo(ScanCandidateStatus.PENDING);
        // combined confidence = vision(0.9) * match(0.8) = 0.72
        assertThat(candidate.confidence()).isEqualTo(0.72);
        // Crucially, nothing is added to the library during a scan.
        verifyNoInteractions(libraryService);
    }

    @Test
    void scanShelf_deduplicatesSameBook_keepingHighestConfidence() {
        Book dune = book(10L, "Dune");
        when(visionService.isAvailable()).thenReturn(true);
        when(visionService.detectShelfBooks(anyString(), eq("image/jpeg"))).thenReturn(List.of(
                new VisionService.DetectedBook("Dune", "Herbert", 0.8),
                new VisionService.DetectedBook("DUNE", "F. Herbert", 1.0)));
        when(bookMatchingService.match("Dune", "Herbert"))
                .thenReturn(new BookMatchingService.MatchResult(dune, 0.9));
        when(bookMatchingService.match("DUNE", "F. Herbert"))
                .thenReturn(new BookMatchingService.MatchResult(dune, 1.0));
        when(scanSessionRepository.save(any(ScanSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ScanResultResponse result = bookScanService.scanShelf(user, new byte[]{9}, "image/jpeg");

        assertThat(result.type()).isEqualTo(ScanType.SHELF);
        assertThat(result.detectedCount()).isEqualTo(1);
        assertThat(result.candidates()).hasSize(1);
        // keeps the higher combined score: 1.0 * 1.0 = 1.0 over 0.8 * 0.9 = 0.72
        assertThat(result.candidates().get(0).confidence()).isEqualTo(1.0);
    }

    @Test
    void confirm_addsConfirmed_skipsOthers_andCompletesSession() {
        ScanSession session = sessionWith(
                candidate(101L, book(10L, "Dune")),
                candidate(102L, book(20L, "1984")));
        when(scanSessionRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(session));
        when(libraryService.addBook(user, 10L, ReadingStatus.READ))
                .thenReturn(userBookResponse(10L, "Dune"));

        ConfirmScanRequest request = new ConfirmScanRequest(List.of(
                new CandidateDecision(101L, true, null, ReadingStatus.READ),
                new CandidateDecision(102L, false, null, null)));

        ConfirmScanResponse response = bookScanService.confirm(user, 7L, request);

        assertThat(response.addedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.alreadyInLibraryCount()).isZero();
        assertThat(response.added()).hasSize(1);
        assertThat(session.getCandidates().get(0).getStatus()).isEqualTo(ScanCandidateStatus.CONFIRMED);
        assertThat(session.getCandidates().get(1).getStatus()).isEqualTo(ScanCandidateStatus.SKIPPED);
        assertThat(session.getStatus()).isEqualTo(ScanSessionStatus.COMPLETED);
        verify(scanSessionRepository).save(session);
    }

    @Test
    void confirm_whenBookAlreadyInLibrary_countsSeparatelyNotAsError() {
        ScanSession session = sessionWith(candidate(101L, book(10L, "Dune")));
        when(scanSessionRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(session));
        when(libraryService.addBook(eq(user), eq(10L), any()))
                .thenThrow(new DuplicateResourceException("already there"));

        ConfirmScanResponse response = bookScanService.confirm(user, 7L,
                new ConfirmScanRequest(List.of(new CandidateDecision(101L, true, null, null))));

        assertThat(response.addedCount()).isZero();
        assertThat(response.alreadyInLibraryCount()).isEqualTo(1);
        assertThat(session.getCandidates().get(0).getStatus()).isEqualTo(ScanCandidateStatus.CONFIRMED);
    }

    @Test
    void confirm_unmatchedCandidateWithoutOverride_throwsBadRequest() {
        ScanSession session = sessionWith(candidate(101L, null));
        when(scanSessionRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> bookScanService.confirm(user, 7L,
                new ConfirmScanRequest(List.of(new CandidateDecision(101L, true, null, null)))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void confirm_unmatchedCandidateWithOverride_usesOverrideBook() {
        ScanSession session = sessionWith(candidate(101L, null));
        Book chosen = book(20L, "Chosen Instead");
        when(scanSessionRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(session));
        when(bookService.getEntityById(20L)).thenReturn(chosen);
        when(libraryService.addBook(user, 20L, ReadingStatus.WANT_TO_READ))
                .thenReturn(userBookResponse(20L, "Chosen Instead"));

        ConfirmScanResponse response = bookScanService.confirm(user, 7L,
                new ConfirmScanRequest(List.of(new CandidateDecision(101L, true, 20L, null))));

        assertThat(response.addedCount()).isEqualTo(1);
        verify(libraryService).addBook(user, 20L, ReadingStatus.WANT_TO_READ);
    }

    // --- helpers -------------------------------------------------------

    private ScanSession sessionWith(ScanCandidate... candidates) {
        ScanSession session = ScanSession.builder()
                .id(7L)
                .user(user)
                .type(ScanType.SHELF)
                .status(ScanSessionStatus.AWAITING_CONFIRMATION)
                .detectedCount(candidates.length)
                .build();
        for (ScanCandidate c : candidates) {
            c.setSession(session);
            session.getCandidates().add(c);
        }
        return session;
    }

    private ScanCandidate candidate(long id, Book matchedBook) {
        return ScanCandidate.builder()
                .id(id)
                .detectedTitle(matchedBook != null ? matchedBook.getTitle() : "Unreadable")
                .detectedAuthor("")
                .visionConfidence(0.9)
                .matchConfidence(matchedBook != null ? 0.9 : 0.0)
                .confidence(matchedBook != null ? 0.81 : 0.0)
                .matchedBook(matchedBook)
                .status(ScanCandidateStatus.PENDING)
                .build();
    }

    private UserBookResponse userBookResponse(long bookId, String title) {
        return new UserBookResponse(
                bookId,
                new BookSummaryResponse(bookId, title, null, List.of()),
                ReadingStatus.WANT_TO_READ,
                null,
                null,
                Instant.now());
    }
}
