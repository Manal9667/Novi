package com.novi.controller;

import com.novi.dto.history.ReadingHistoryResponse;
import com.novi.security.CurrentUserProvider;
import com.novi.service.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reading-history")
@RequiredArgsConstructor
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<ReadingHistoryResponse> getHistory() {
        return readingHistoryService.getHistory(currentUserProvider.getCurrentUser());
    }
}
