package com.internalmarketplace.api.category;

import com.internalmarketplace.api.category.dto.CategoryResponse;
import com.internalmarketplace.api.category.dto.UpdateCategoryRequest;
import com.internalmarketplace.api.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Business rules for categories, a direct port of category.service.js.
 * Audit logging intentionally stays in the controller (see
 * CategoryController) to preserve a subtle existing behavior: categories
 * auto-created by an approved category request (CategoryRequestService)
 * call {@link #createCategory} directly and do NOT produce a
 * CATEGORY_CREATED audit entry today, matching categoryRequests.service.js.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.listActive();
    }

    public String createCategory(String name, String description, String createdBy) {
        if (categoryRepository.findActiveByName(name).isPresent()) {
            throw new ConflictException("A category with this name is already active");
        }
        return categoryRepository.create(name, description, createdBy);
    }

    public void updateCategory(String id, UpdateCategoryRequest patch) {
        Map<String, Object> updates = new LinkedHashMap<>();
        if (patch.name() != null) {
            updates.put("name", patch.name());
        }
        if (patch.description() != null) {
            updates.put("description", patch.description());
        }
        if (patch.status() != null) {
            updates.put("status", patch.status().name());
        }
        categoryRepository.update(id, updates);
    }

    public CategoryResponse getCategory(String id) {
        return categoryRepository.findById(id).orElse(null);
    }
}
