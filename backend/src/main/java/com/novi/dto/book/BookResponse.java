package com.novi.dto.book;

import java.time.LocalDate;
import java.util.List;

public record BookResponse(
        Long id,
        String title,
        String description,
        String coverImageUrl,
        String isbn,
        LocalDate publicationDate,
        List<AuthorDto> authors,
        List<GenreDto> genres,
        Double averageRating,
        long ratingCount
) {
}
