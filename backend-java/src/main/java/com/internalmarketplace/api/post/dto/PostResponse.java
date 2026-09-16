package com.internalmarketplace.api.post.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Mirrors the raw posts/{id} Firestore document. {@code imageUrl} is only
 * populated by list responses (post.service.js's listPosts) and
 * {@code images} only by the single-post response (getPost) — matching the
 * Node service's dynamic shape; the unused one stays null and is dropped
 * from the JSON body by the app-wide non_null Jackson inclusion setting.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PostResponse(
        String id,
        String categoryId,
        String userId,
        String userEmail,
        String userName,
        String title,
        String description,
        String postType,
        Double price,
        List<String> tags,
        String expiryDate,
        String status,
        List<String> searchKeywords,
        String coverImageKey,
        Timestamp createdAt,
        Timestamp updatedAt,
        String imageUrl,
        List<PostImageResponse> images
) {
    @SuppressWarnings("unchecked")
    public static PostResponse fromSnapshot(DocumentSnapshot doc) {
        Object tags = doc.get("tags");
        Object keywords = doc.get("search_keywords");
        return new PostResponse(
                doc.getId(),
                doc.getString("category_id"),
                doc.getString("user_id"),
                null,
                null,
                doc.getString("title"),
                doc.getString("description"),
                doc.getString("post_type"),
                doc.getDouble("price"),
                tags instanceof List ? (List<String>) tags : List.of(),
                doc.getString("expiry_date"),
                doc.getString("status"),
                keywords instanceof List ? (List<String>) keywords : List.of(),
                doc.getString("cover_image_key"),
                doc.getTimestamp("created_at"),
                doc.getTimestamp("updated_at"),
                null,
                null);
    }

    public PostResponse withAuthor(String email, String name) {
        return new PostResponse(id, categoryId, userId, email, name, title, description, postType, price, tags,
                expiryDate, status, searchKeywords, coverImageKey, createdAt, updatedAt, imageUrl, images);
    }

    public PostResponse withImageUrl(String signedUrl) {
        return new PostResponse(id, categoryId, userId, userEmail, userName, title, description, postType, price,
                tags, expiryDate, status, searchKeywords, coverImageKey, createdAt, updatedAt, signedUrl, images);
    }

    public PostResponse withImages(List<PostImageResponse> resolvedImages) {
        return new PostResponse(id, categoryId, userId, userEmail, userName, title, description, postType, price,
                tags, expiryDate, status, searchKeywords, coverImageKey, createdAt, updatedAt, imageUrl,
                resolvedImages);
    }
}
