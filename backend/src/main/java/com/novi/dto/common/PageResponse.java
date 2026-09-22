package com.novi.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A stable, explicit pagination envelope used by every collection endpoint.
 *
 * <p>Spring's own {@code Page} serialization is discouraged (its JSON shape is
 * an implementation detail and emits a warning in Spring Boot 3.3+), so we map
 * to this small record instead. Every list endpoint returns the same shape,
 * which also resolves the previous inconsistency where book browse returned a
 * page but search returned a bare array.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Wraps an already-materialized list as a single page. Used for live
     * external search results, which aren't paginated at the source but should
     * still present the same envelope as every other collection endpoint.
     */
    public static <T> PageResponse<T> ofSinglePage(List<T> content) {
        return new PageResponse<>(content, 0, content.size(), content.size(), 1, true);
    }
}
