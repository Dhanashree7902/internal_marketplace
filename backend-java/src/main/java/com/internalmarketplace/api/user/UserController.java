package com.internalmarketplace.api.user;

import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import com.internalmarketplace.api.user.UserRepository.EmployeeProfile;
import com.internalmarketplace.api.user.dto.EmployeeSummaryResponse;
import com.internalmarketplace.api.user.dto.MeResponse;
import com.internalmarketplace.api.user.dto.RoleAssignmentResponse;
import com.internalmarketplace.api.roleremoval.RoleRemovalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuditService auditService;
    private final RoleRemovalRequestRepository roleRemovalRequestRepository;

    // GET /api/v1/me
    @GetMapping("/me")
    public MeResponse getMe(@CurrentUser FirebaseUserPrincipal user) {
        return new MeResponse(
                user.uid(), user.employeeId(), user.email(), user.name(), user.role(), user.status());
    }

    // GET /api/v1/admin/users?query=...
    @GetMapping("/admin/users")
    public DataResponse<List<EmployeeSummaryResponse>> searchEmployees(
            @RequestParam(required = false) String query) {
        return DataResponse.of(userService.searchEmployees(query));
    }

    // POST /api/v1/admin/users/{uid}/promote
    @PostMapping("/admin/users/{uid}/promote")
    public ResponseEntity<Void> promote(@PathVariable String uid, @CurrentUser FirebaseUserPrincipal user) {
        EmployeeProfile target = userService.promoteToAdmin(uid, user);

        auditService.recordAudit(user.uid(), "ADMIN_ROLE_ASSIGNED", "user", uid, Map.of(
                "assignedByName", user.name(),
                "assignedToName", target.name(),
                "assignedToEmail", target.email()));

        // Create a pending role-removal request record to surface in the Admin UI.
        try {
            roleRemovalRequestRepository.create(target.uid(), target.name(), target.email());
        } catch (Exception ignored) {
            // Non-fatal: ignore repository failures when backfilling.
        }

        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/admin/users/role-assignments
    @GetMapping("/admin/users/role-assignments")
    public DataResponse<List<RoleAssignmentResponse>> roleAssignments() {
        return DataResponse.of(userService.listRecentRoleAssignments());
    }
}
