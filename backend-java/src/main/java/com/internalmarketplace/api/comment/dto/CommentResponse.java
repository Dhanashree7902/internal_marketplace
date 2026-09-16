package com.internalmarketplace.api.comment.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CommentResponse(
        String id,
        String postId,
        String userId,
        String userEmail,
        String text,
        String status,
        Timestamp createdAt,
        Timestamp updatedAt
) {
    public static CommentResponse fromSnapshot(DocumentSnapshot doc) {
        return new CommentResponse(
                doc.getId(),
                doc.getString("post_id"),
                doc.getString("user_id"),
                doc.getString("user_email"),
                doc.getString("text"),
                doc.getString("status"),
                doc.getTimestamp("created_at"),
                doc.getTimestamp("updated_at"));
    }

    public CommentResponse withUserEmail(String email) {
        return new CommentResponse(id, postId, userId, email, text, status, createdAt, updatedAt);
    }
}
