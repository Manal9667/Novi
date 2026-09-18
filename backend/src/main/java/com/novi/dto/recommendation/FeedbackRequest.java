package com.novi.dto.recommendation;

import com.novi.entity.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(
        @NotNull FeedbackType feedbackType,
        String reason // e.g. "Too slow", "Too much romance" - only meaningful for NOT_FOR_ME
) {
}
