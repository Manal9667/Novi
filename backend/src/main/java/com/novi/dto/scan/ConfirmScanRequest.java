package com.novi.dto.scan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** The set of per-candidate decisions the user made after reviewing a scan. */
public record ConfirmScanRequest(
        @NotEmpty(message = "At least one decision is required")
        @Valid
        List<CandidateDecision> decisions
) {
}
