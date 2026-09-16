package com.internalmarketplace.api.roleremoval;

import com.internalmarketplace.api.common.exception.ConflictException;
import com.internalmarketplace.api.common.exception.ForbiddenException;
import com.internalmarketplace.api.common.exception.NotFoundException;
import com.internalmarketplace.api.notification.NotificationRepository;
import com.internalmarketplace.api.roleremoval.dto.RoleRemovalRequestResponse;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import com.internalmarketplace.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Business rules for the Admin self-service role-removal request workflow. */
@Service
@RequiredArgsConstructor
public class RoleRemovalRequestService {

    private final RoleRemovalRequestRepository roleRemovalRequestRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public String createRequest(FirebaseUserPrincipal requester) {
        List<String> otherAdmins = new ArrayList<>(userRepository.findActiveAdminUids());
        otherAdmins.remove(requester.uid());

        if (otherAdmins.isEmpty()) {
            throw new ConflictException(
                    "You are the only active Admin. Promote another employee to Admin before requesting removal.");
        }
        if (roleRemovalRequestRepository.existsPendingFor(requester.uid())) {
            throw new ConflictException("You already have a pending removal request.");
        }

        String id = roleRemovalRequestRepository.create(requester.uid(), requester.name(), requester.email());

        String message = requester.name() + " has requested to remove their own Admin access.";
        notificationRepository.createForMany(otherAdmins, "ADMIN_ROLE_REMOVAL_REQUESTED", id, message);

        return id;
    }

    public List<RoleRemovalRequestResponse> listPendingRequests() {
        return roleRemovalRequestRepository.listPending();
    }

    public RoleRemovalRequestResponse decide(String id, String status, FirebaseUserPrincipal reviewer,
                                              String reviewNote) {
        RoleRemovalRequestResponse request = roleRemovalRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role removal request not found"));
        if (!"PENDING".equals(request.status())) {
            throw new ConflictException("This request has already been reviewed");
        }
        if (reviewer.uid().equals(request.requestedByUid())) {
            throw new ForbiddenException("You cannot review your own removal request");
        }

        roleRemovalRequestRepository.updateDecision(id, status, reviewer.uid(), reviewNote);

        String message;
        if ("APPROVED".equals(status)) {
            userRepository.updateRole(request.requestedByUid(), "employee");
            message = "Your request to remove Admin access was approved. You are now an Employee.";
        } else {
            message = "Your request to remove Admin access was rejected by " + reviewer.name()
                    + ". You remain an Admin.";
        }
        notificationRepository.create(request.requestedByUid(), "ADMIN_ROLE_" + status, id, message);

        return request;
    }

    public void cancelRequest(String id, FirebaseUserPrincipal requester) {
        RoleRemovalRequestResponse request = roleRemovalRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role removal request not found"));
        if (!requester.uid().equals(request.requestedByUid())) {
            throw new ForbiddenException("You can only cancel your own removal request");
        }
        if (!"PENDING".equals(request.status())) {
            throw new ConflictException("This request has already been reviewed");
        }
        roleRemovalRequestRepository.updateDecision(id, "CANCELLED", requester.uid(), "Cancelled by requester");
    }
}
