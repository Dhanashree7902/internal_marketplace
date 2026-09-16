package com.internalmarketplace.api.category;

import com.internalmarketplace.api.category.dto.CategoryResponse;
import com.internalmarketplace.api.category.dto.CreateCategoryRequest;
import com.internalmarketplace.api.category.dto.UpdateCategoryRequest;
import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.common.web.IdResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final AuditService auditService;

    // GET /api/v1/categories
    @GetMapping("/categories")
    public DataResponse<java.util.List<CategoryResponse>> listCategories() {
        return DataResponse.of(categoryService.listActiveCategories());
    }

    // POST /api/v1/admin/categories
    @PostMapping("/admin/categories")
    public ResponseEntity<IdResponse> createCategory(@Valid @RequestBody CreateCategoryRequest body,
                                                       @CurrentUser FirebaseUserPrincipal user) {
        String id = categoryService.createCategory(body.name(), body.description(), user.uid());

        auditService.recordAudit(user.uid(), "CATEGORY_CREATED", "category", id, Map.of("name", body.name()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    // PATCH /api/v1/admin/categories/{id}
    @PatchMapping("/admin/categories/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable String id,
                                                @Valid @RequestBody UpdateCategoryRequest body,
                                                @CurrentUser FirebaseUserPrincipal user) {
        categoryService.updateCategory(id, body);

        auditService.recordAudit(user.uid(), "CATEGORY_UPDATED", "category", id, patchMetadata(body));

        return ResponseEntity.noContent().build();
    }

    private static Map<String, Object> patchMetadata(UpdateCategoryRequest body) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (body.name() != null) {
            metadata.put("name", body.name());
        }
        if (body.description() != null) {
            metadata.put("description", body.description());
        }
        if (body.status() != null) {
            metadata.put("status", body.status().name());
        }
        return metadata;
    }
}
