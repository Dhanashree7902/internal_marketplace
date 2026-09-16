package com.internalmarketplace.api.category.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

/** Mirrors the raw categories/{id} Firestore document, matching {id, ...doc.data()}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryResponse(
        String id,
        String name,
        String description,
        String status,
        String createdBy,
        Timestamp createdAt,
        Timestamp updatedAt
) {
    public static CategoryResponse fromSnapshot(DocumentSnapshot doc) {
        return new CategoryResponse(
                doc.getId(),
                doc.getString("name"),
                doc.getString("description"),
                doc.getString("status"),
                doc.getString("created_by"),
                doc.getTimestamp("created_at"),
                doc.getTimestamp("updated_at"));
    }
}
