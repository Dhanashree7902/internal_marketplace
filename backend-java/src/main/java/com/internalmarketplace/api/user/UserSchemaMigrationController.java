package com.internalmarketplace.api.user;

import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import com.internalmarketplace.api.user.dto.UserSchemaMigrationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class UserSchemaMigrationController {

    private final UserSchemaMigrationService migrationService;
    private final AuditService auditService;

    // POST /api/v1/admin/migrations/users-schema?dry_run=true (default: dry run, no writes)
    @PostMapping("/migrations/users-schema")
    public DataResponse<UserSchemaMigrationResult> migrateUserSchema(
            @RequestParam(name = "dry_run", defaultValue = "true") boolean dryRun,
            @CurrentUser FirebaseUserPrincipal user) {
        UserSchemaMigrationResult result = migrationService.migrate(dryRun);

        if (!dryRun) {
            auditService.recordAudit(user.uid(), "USER_SCHEMA_MIGRATION_RUN", "user", "batch", Map.of(
                    "usersScanned", result.usersScanned(),
                    "usersUpdated", result.usersUpdated(),
                    "employeeIdConflicts", result.employeeIdConflicts().size(),
                    "unresolvedUsers", result.unresolvedUsers().size()));
        }

        return DataResponse.of(result);
    }
}
