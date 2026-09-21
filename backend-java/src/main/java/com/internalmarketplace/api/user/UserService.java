package com.internalmarketplace.api.user;

import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.common.exception.ConflictException;
import com.internalmarketplace.api.common.exception.NotFoundException;
import com.internalmarketplace.api.notification.NotificationRepository;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import com.internalmarketplace.api.user.UserRepository.EmployeeProfile;
import com.internalmarketplace.api.user.dto.EmployeeSummaryResponse;
import com.internalmarketplace.api.user.dto.RoleAssignmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Business rules for Admin privilege assignment. */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int MAX_ROLE_ASSIGNMENTS = 20;

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    public List<EmployeeSummaryResponse> searchEmployees(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String needle = query.trim().toLowerCase();
        return userRepository.listActiveEmployees().stream()
                .filter(e -> containsIgnoreCase(e.name(), needle) || containsIgnoreCase(e.email(), needle))
                .limit(MAX_SEARCH_RESULTS)
                .toList();
    }

    public EmployeeProfile promoteToAdmin(String targetUid, FirebaseUserPrincipal actor) {
        EmployeeProfile target = userRepository.findProfileById(targetUid)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean wasAdmin = "admin".equals(target.role());
        if (!"ACTIVE".equals(target.status())) {
            throw new ConflictException("Cannot promote an inactive user");
        }

        if (!wasAdmin) {
            userRepository.updateRole(targetUid, "admin");

            String message = "You have been granted Admin access by " + actor.name() + ".";
            notificationRepository.create(targetUid, "ADMIN_ROLE_ASSIGNED", targetUid, message);
        }

        return target;
    }

    public List<RoleAssignmentResponse> listRecentRoleAssignments() {
        return auditService.listByAction("ADMIN_ROLE_ASSIGNED", MAX_ROLE_ASSIGNMENTS).stream()
                .map(entry -> {
                    Map<String, Object> metadata = entry.metadata();
                    return new RoleAssignmentResponse(
                            stringValue(metadata.get("assignedByName")),
                            stringValue(metadata.get("assignedToName")),
                            stringValue(metadata.get("assignedToEmail")),
                            entry.createdAt());
                })
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
