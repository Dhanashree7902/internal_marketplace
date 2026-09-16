package com.internalmarketplace.api.categoryrequest;

import com.internalmarketplace.api.categoryrequest.dto.CategoryRequestCreateRequest;
import com.internalmarketplace.api.categoryrequest.dto.CategoryRequestResponse;
import com.internalmarketplace.api.categoryrequest.dto.CategoryRequestReviewRequest;
import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.common.exception.ConflictException;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.common.web.IdResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryRequestController {

    private final CategoryRequestService categoryRequestService;
    private final AuditService auditService;

    // POST /api/v1/category-requests
    @PostMapping("/category-requests")
    public ResponseEntity<IdResponse> createCategoryRequest(@Valid @RequestBody CategoryRequestCreateRequest body,
                                                             @CurrentUser FirebaseUserPrincipal user) {
        if (categoryRequestService.hasPendingOrActiveRequest(body.proposedName())) {
            throw new ConflictException("A matching active category or pending request already exists");
        }
        String id = categoryRequestService.createRequest(user.uid(), user.isAdmin(), body.proposedName(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    // GET /api/v1/admin/category-requests
    @GetMapping("/admin/category-requests")
    public DataResponse<List<CategoryRequestResponse>> listCategoryRequests() {
        return DataResponse.of(categoryRequestService.listPendingRequests());
    }

    // POST /api/v1/admin/category-requests/{id}/approve
    @PostMapping("/admin/category-requests/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable String id,
                                         @Valid @RequestBody(required = false) CategoryRequestReviewRequest body,
                                         @CurrentUser FirebaseUserPrincipal user) {
        return decide(id, "APPROVED", body, user);
    }

    // POST /api/v1/admin/category-requests/{id}/reject
    @PostMapping("/admin/category-requests/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable String id,
                                        @Valid @RequestBody(required = false) CategoryRequestReviewRequest body,
                                        @CurrentUser FirebaseUserPrincipal user) {
        return decide(id, "REJECTED", body, user);
    }

    private ResponseEntity<Void> decide(String id, String status, CategoryRequestReviewRequest body,
                                         FirebaseUserPrincipal user) {
        String reviewNote = body != null ? body.reviewNote() : "";
        var request = categoryRequestService.decide(id, status, user.uid(), reviewNote);

        auditService.recordAudit(user.uid(), "CATEGORY_REQUEST_" + status, "categoryRequest", id,
                Map.of("proposedName", request.proposedName()));

        return ResponseEntity.noContent().build();
    }
}
