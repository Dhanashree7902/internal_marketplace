package com.internalmarketplace.api.roleremoval;

import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.common.web.IdResponse;
import com.internalmarketplace.api.roleremoval.dto.RoleRemovalRequestResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/role-removal-requests")
@RequiredArgsConstructor
public class RoleRemovalRequestController {

    private final RoleRemovalRequestService roleRemovalRequestService;
    private final AuditService auditService;

    // POST /api/v1/admin/role-removal-requests
    @PostMapping
    public ResponseEntity<IdResponse> createRequest(@CurrentUser FirebaseUserPrincipal user) {
        String id = roleRemovalRequestService.createRequest(user);

        auditService.recordAudit(user.uid(), "ADMIN_ROLE_REMOVAL_REQUESTED", "user", user.uid());

        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    // GET /api/v1/admin/role-removal-requests
    @GetMapping
    public DataResponse<List<RoleRemovalRequestResponse>> listRequests() {
        return DataResponse.of(roleRemovalRequestService.listPendingRequests());
    }

    // POST /api/v1/admin/role-removal-requests/{id}/approve
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable String id, @CurrentUser FirebaseUserPrincipal user) {
        return decide(id, "APPROVED", user);
    }

    // POST /api/v1/admin/role-removal-requests/{id}/reject
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable String id, @CurrentUser FirebaseUserPrincipal user) {
        return decide(id, "REJECTED", user);
    }

    private ResponseEntity<Void> decide(String id, String status, FirebaseUserPrincipal user) {
        RoleRemovalRequestResponse request = roleRemovalRequestService.decide(id, status, user, "");

        auditService.recordAudit(user.uid(), "ADMIN_ROLE_" + status, "user", request.requestedByUid(), Map.of(
                "requestedByName", request.requestedByName(),
                "reviewedByName", user.name()));

        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/admin/role-removal-requests/{id}/cancel
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String id, @CurrentUser FirebaseUserPrincipal user) {
        roleRemovalRequestService.cancelRequest(id, user);

        auditService.recordAudit(user.uid(), "ADMIN_ROLE_REMOVAL_CANCELLED", "user", user.uid());

        return ResponseEntity.noContent().build();
    }
}
