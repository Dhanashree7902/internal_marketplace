package com.internalmarketplace.api.categoryrequest.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryRequestResponse(
        String id,
        String requestedBy,
        String proposedName,
        String reason,
        String status,
        String reviewedBy,
        String reviewNote,
        Timestamp createdAt,
        Timestamp updatedAt
) {
    public static CategoryRequestResponse fromSnapshot(DocumentSnapshot doc) {
        return new CategoryRequestResponse(
                doc.getId(),
                doc.getString("requested_by"),
                doc.getString("proposed_name"),
                doc.getString("reason"),
                doc.getString("status"),
                doc.getString("reviewed_by"),
                doc.getString("review_note"),
                doc.getTimestamp("created_at"),
                doc.getTimestamp("updated_at"));
    }
}
