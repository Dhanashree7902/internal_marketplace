package com.internalmarketplace.api.categoryrequest;

import com.internalmarketplace.api.category.CategoryRepository;
import com.internalmarketplace.api.category.CategoryService;
import com.internalmarketplace.api.categoryrequest.dto.CategoryRequestResponse;
import com.internalmarketplace.api.common.exception.ConflictException;
import com.internalmarketplace.api.common.exception.NotFoundException;
import com.internalmarketplace.api.notification.NotificationRepository;
import com.internalmarketplace.api.notification.event.CategoryRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/** Business rules for category requests, a direct port of categoryRequests.service.js. */
@Service
@RequiredArgsConstructor
public class CategoryRequestService {

    private final CategoryRequestRepository categoryRequestRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public boolean hasPendingOrActiveRequest(String proposedName) {
        if (categoryRepository.findActiveByName(proposedName).isPresent()) {
            return true;
        }
        return categoryRequestRepository.existsPendingByProposedName(proposedName);
    }

    public String createRequest(String requestedBy, boolean requestedByAdmin, String proposedName, String reason) {
        String id = categoryRequestRepository.create(requestedBy, proposedName, reason);

        if (requestedByAdmin) {
            // Admins don't need their own proposals reviewed -- approve and create
            // the category immediately, same as CategoryService#createCategory
            // called from decide().
            categoryRequestRepository.updateDecision(id, "APPROVED", requestedBy,
                    "Auto-approved: submitted by an admin");
            categoryService.createCategory(proposedName, reason, requestedBy);
        } else {
            // Fans out to admins asynchronously; replaces functions/src/index.js's onCategoryRequestCreate.
            eventPublisher.publishEvent(new CategoryRequestCreatedEvent(id, requestedBy, proposedName));
        }
        return id;
    }

    public List<CategoryRequestResponse> listPendingRequests() {
        return categoryRequestRepository.listPending();
    }

    /**
     * Approves or rejects a request. When approved, creates the category via
     * {@link CategoryService#createCategory} directly — bypassing
     * CategoryController's audit call, matching categoryRequests.service.js's
     * decideRequest, which imports and calls category.service.js's
     * createCategory bare (no CATEGORY_CREATED audit entry for
     * request-approved categories today).
     *
     * <p>The requester-facing notification is written synchronously here
     * (not via the async admin-fanout event) because the requester needs the
     * result as part of this same request/response cycle — same rationale as
     * the Node comment in categoryRequests.controller.js.
     */
    public CategoryRequestResponse decide(String id, String status, String reviewedBy, String reviewNote) {
        CategoryRequestResponse request = categoryRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category request not found"));
        if (!"PENDING".equals(request.status())) {
            throw new ConflictException("This request has already been reviewed");
        }

        categoryRequestRepository.updateDecision(id, status, reviewedBy, reviewNote);

        if ("APPROVED".equals(status)) {
            categoryService.createCategory(request.proposedName(), request.reason(), reviewedBy);
        }

        String message = "APPROVED".equals(status)
                ? "Your category request \"" + request.proposedName() + "\" was approved."
                : "Your category request \"" + request.proposedName() + "\" was rejected.";
        notificationRepository.create(request.requestedBy(), "CATEGORY_" + status, id, message);

        return request;
    }
}
