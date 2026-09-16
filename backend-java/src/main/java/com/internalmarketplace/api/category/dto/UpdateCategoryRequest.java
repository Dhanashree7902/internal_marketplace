package com.internalmarketplace.api.category.dto;

import com.internalmarketplace.api.category.CategoryStatus;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 2, max = 100) String name,
        @Size(max = 2000) String description,
        CategoryStatus status
) {
}
