package com.internalmarketplace.api.categoryrequest.dto;

import jakarta.validation.constraints.Size;

public record CategoryRequestReviewRequest(
        @Size(max = 2000) String reviewNote
) {
    public CategoryRequestReviewRequest {
        if (reviewNote == null) {
            reviewNote = "";
        }
    }
}
