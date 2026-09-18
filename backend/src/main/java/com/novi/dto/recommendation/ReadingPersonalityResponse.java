package com.novi.dto.recommendation;

import java.util.List;

public record ReadingPersonalityResponse(
        List<AffinityEntry> genreAffinities,
        List<AffinityEntry> themeAffinities,
        String summary
) {
}
