package com.novi.dto.scan;

import com.novi.entity.enums.ScanSessionStatus;
import com.novi.entity.enums.ScanType;

import java.util.List;

/** The result of a scan: the persisted session plus every detected candidate. */
public record ScanResultResponse(
        Long sessionId,
        ScanType type,
        int detectedCount,
        ScanSessionStatus status,
        List<ScanCandidateResponse> candidates
) {
}
