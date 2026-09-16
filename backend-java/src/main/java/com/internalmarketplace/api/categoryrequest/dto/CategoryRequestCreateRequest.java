package com.internalmarketplace.api.categoryrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestCreateRequest(
        @NotBlank @Size(min = 2, max = 100) String proposedName,
        @NotBlank @Size(min = 1, max = 2000) String reason
) {
}
