package com.internalmarketplace.api.moderation.dto;

import com.internalmarketplace.api.moderation.PostModerationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModeratePostRequest(
        @NotNull PostModerationStatus status,
        @Size(max = 2000) String note
) {
}
