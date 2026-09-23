package com.internalmarketplace.api.post.dto;

import com.internalmarketplace.api.common.validation.Iso8601Instant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * postType is free text, not a closed enum: the Create Post form offers a
 * fixed dropdown of listing categories (Buy/Sell, Rentals, Services, ...)
 * plus an "Other" option that lets the user type a custom listing type, so
 * the value actually saved can be either one of those labels or arbitrary
 * user-entered text.
 */
public record CreatePostRequest(
        @NotBlank String categoryId,
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 1, max = 5000) String description,
        @NotBlank @Size(max = 40) String postType,
        @DecimalMin(value = "0", message = "Price cannot be negative") Double price,
        @Size(max = 20) List<String> tags,
        @Iso8601Instant String expiryDate
) {
    public CreatePostRequest {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
