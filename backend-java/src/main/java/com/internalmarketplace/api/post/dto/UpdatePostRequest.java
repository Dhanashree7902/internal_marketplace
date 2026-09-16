package com.internalmarketplace.api.post.dto;

import com.internalmarketplace.api.post.PostUpdateStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Partial update: an absent field leaves the stored value untouched (matches
 * Firestore's document.update() semantics used in post.service.js). A null
 * {@code price} is treated the same as "not provided" rather than "clear the
 * price" — the one real caller (frontend/src/pages/MyPosts.jsx) only ever
 * PATCHes {@code status}, so this never diverges from zod's stricter
 * absent-vs-null distinction in practice.
 */
public record UpdatePostRequest(
        @Size(min = 3, max = 200) String title,
        @Size(min = 1, max = 5000) String description,
        @DecimalMin(value = "0", message = "Price cannot be negative") Double price,
        PostUpdateStatus status
) {
}
