package com.internalmarketplace.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Size(max = 2000) String description
) {
    public CreateCategoryRequest {
        if (description == null) {
            description = "";
        }
    }
}
