package com.internalmarketplace.api.post.dto;

import com.internalmarketplace.api.common.validation.Iso8601Instant;
import com.internalmarketplace.api.post.PostType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotBlank String categoryId,
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 1, max = 5000) String description,
        @NotNull PostType postType,
        @DecimalMin(value = "0", message = "Price cannot be negative") Double price,
        @Size(max = 20) List<String> tags,
        @Iso8601Instant String expiryDate
) {
    public CreatePostRequest {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
