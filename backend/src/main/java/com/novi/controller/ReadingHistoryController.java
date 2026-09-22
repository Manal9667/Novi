package com.novi.controller;

import com.novi.dto.common.PageResponse;
import com.novi.dto.history.ReadingHistoryResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reading-history")
@RequiredArgsConstructor
public class ReadingHistoryController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final ReadingHistoryService readingHistoryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PageResponse<ReadingHistoryResponse> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        PageRequest pageable = PageRequest.of(Math.max(0, page), boundedSize, Sort.by("occurredAt").descending());
        return readingHistoryService.getHistory(currentUserProvider.getCurrentUser(), pageable);
    }
}
