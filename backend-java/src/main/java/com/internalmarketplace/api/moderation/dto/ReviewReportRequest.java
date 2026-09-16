package com.internalmarketplace.api.moderation.dto;

import com.internalmarketplace.api.moderation.ReportReviewStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewReportRequest(
        @NotNull ReportReviewStatus status
) {
}
