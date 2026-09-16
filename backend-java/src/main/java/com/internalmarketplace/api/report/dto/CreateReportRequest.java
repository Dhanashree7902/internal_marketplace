package com.internalmarketplace.api.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank @Pattern(regexp = "^(post|comment)$", message = "must be 'post' or 'comment'") String targetType,
        @NotBlank String targetId,
        @NotBlank @Size(min = 1, max = 2000) String reason
) {
}
