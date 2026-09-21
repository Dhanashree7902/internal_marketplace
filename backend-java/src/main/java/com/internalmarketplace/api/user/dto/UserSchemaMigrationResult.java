package com.internalmarketplace.api.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserSchemaMigrationResult(
        boolean dryRun,
        int usersScanned,
        int usersUpdated,
        List<UpdatedUser> updatedUsers,
        List<String> employeeIdConflicts,
        List<String> unresolvedUsers
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdatedUser(String uid, List<String> fieldsChanged) {
    }
}
