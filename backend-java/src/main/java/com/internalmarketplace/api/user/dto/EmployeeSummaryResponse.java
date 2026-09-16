package com.internalmarketplace.api.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** A candidate for Admin promotion, returned by the employee search endpoint. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmployeeSummaryResponse(
        String uid,
        String name,
        String email,
        String employeeId,
        String department
) {
}
