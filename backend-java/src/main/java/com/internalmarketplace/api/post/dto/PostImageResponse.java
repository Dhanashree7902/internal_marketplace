package com.internalmarketplace.api.post.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PostImageResponse(
        String id,
        String objectKey,
        long sortOrder,
        Map<String, Object> metadata,
        Timestamp createdAt,
        String url
) {
    public static PostImageResponse fromSnapshot(DocumentSnapshot doc) {
        Long sortOrder = doc.getLong("sort_order");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
        return new PostImageResponse(
                doc.getId(),
                doc.getString("object_key"),
                sortOrder != null ? sortOrder : 0,
                metadata,
                doc.getTimestamp("created_at"),
                null);
    }

    public PostImageResponse withUrl(String signedUrl) {
        return new PostImageResponse(id, objectKey, sortOrder, metadata, createdAt, signedUrl);
    }
}
