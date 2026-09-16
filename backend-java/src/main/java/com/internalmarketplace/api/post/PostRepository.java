package com.internalmarketplace.api.post;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Firestore;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import com.internalmarketplace.api.post.dto.PostImageResponse;
import com.internalmarketplace.api.post.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin Firestore wrapper, a direct analogue of post.service.js. */
@Repository
@RequiredArgsConstructor
public class PostRepository {

    private static final int PAGE_SIZE = 20;

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.POSTS);
    }

    public PagedResult listPosts(String categoryId, String status, String cursor, List<String> searchTerms) {
        Query query = collection().whereEqualTo("status", status);
        if (categoryId != null && !categoryId.isBlank()) {
            query = query.whereEqualTo("category_id", categoryId);
        }
        if (!searchTerms.isEmpty()) {
            query = query.whereArrayContainsAny("search_keywords", searchTerms);
        }
        query = query.orderBy("created_at", Query.Direction.DESCENDING);

        if (cursor != null && !cursor.isBlank()) {
            DocumentSnapshot cursorDoc = FirestoreSupport.await(collection().document(cursor).get());
            if (cursorDoc.exists()) {
                query = query.startAfter(cursorDoc);
            }
        }

        QuerySnapshot snap = FirestoreSupport.await(query.limit(PAGE_SIZE).get());
        List<QueryDocumentSnapshot> docs = snap.getDocuments();
        List<PostResponse> items = docs.stream().map(PostResponse::fromSnapshot).toList();
        String nextCursor = docs.size() == PAGE_SIZE ? docs.get(docs.size() - 1).getId() : null;
        return new PagedResult(items, nextCursor);
    }

    public Optional<PostResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(PostResponse.fromSnapshot(doc)) : Optional.empty();
    }

    public List<PostImageResponse> listImages(String postId) {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().document(postId)
                        .collection(FirestoreCollections.POST_IMAGES_SUBCOLLECTION)
                        .orderBy("sort_order")
                        .get());
        return snap.getDocuments().stream().map(PostImageResponse::fromSnapshot).toList();
    }

    public String create(Map<String, Object> data) {
        DocumentReference ref = FirestoreSupport.await(collection().add(data));
        return ref.getId();
    }

    public void update(String id, Map<String, Object> patch) {
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>(patch);
        data.put("updated_at", FieldValue.serverTimestamp());
        FirestoreSupport.await(collection().document(id).update(data));
    }

    public void updateStatus(String id, String status) {
        FirestoreSupport.await(collection().document(id).update(
                "status", status,
                "updated_at", FieldValue.serverTimestamp()));
    }

    public String addImage(String postId, Map<String, Object> imageData) {
        DocumentReference ref = FirestoreSupport.await(
                collection().document(postId).collection(FirestoreCollections.POST_IMAGES_SUBCOLLECTION).add(imageData));
        return ref.getId();
    }

    public void setCoverImageKey(String postId, String objectKey) {
        FirestoreSupport.await(collection().document(postId).update("cover_image_key", objectKey));
    }

    public record PagedResult(List<PostResponse> items, String nextCursor) {
    }
}
