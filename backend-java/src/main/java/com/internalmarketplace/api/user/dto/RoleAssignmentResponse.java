package com.internalmarketplace.api.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;

/** One row of the "Recent Admin Assignments" audit trail shown in the Admin Dashboard. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoleAssignmentResponse(
        String assignedByName,
        String assignedToName,
        String assignedToEmail,
        Timestamp createdAt
) {
}
