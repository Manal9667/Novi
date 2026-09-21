package com.novi.dto.scan;

import com.novi.dto.library.UserBookResponse;

import java.util.List;

/** Summary of what a confirm request did to the user's library. */
public record ConfirmScanResponse(
        int addedCount,
        int skippedCount,
        int alreadyInLibraryCount,
        List<UserBookResponse> added
) {
}
