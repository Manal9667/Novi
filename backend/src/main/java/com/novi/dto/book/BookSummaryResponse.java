package com.novi.dto.book;

import java.util.List;

public record BookSummaryResponse(
        Long id,
        String title,
        String coverImageUrl,
        List<String> authorNames
) {
}
