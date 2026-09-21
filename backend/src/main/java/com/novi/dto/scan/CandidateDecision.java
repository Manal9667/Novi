package com.novi.dto.scan;

import com.novi.entity.enums.ReadingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * The user's decision about one scanned candidate.
 * <ul>
 *   <li>{@code confirm=false} - skip this book (nothing is added).</li>
 *   <li>{@code confirm=true} - add it to the library. Uses {@code overrideBookId}
 *       if the user picked a different book ("Choose Another"), otherwise the
 *       candidate's own matched book. {@code status} defaults to WANT_TO_READ.</li>
 * </ul>
 */
public record CandidateDecision(
        @NotNull Long candidateId,
        boolean confirm,
        Long overrideBookId,
        ReadingStatus status
) {
}
