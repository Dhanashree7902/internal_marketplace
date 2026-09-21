package com.internalmarketplace.api.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Matches GET /me's hand-picked field subset exactly (no created_at leak). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MeResponse(
        String uid,
        String employeeId,
        String email,
        String name,
        String role,
        String status
) {
}
