package com.internalmarketplace.api.post;

import com.google.api.core.ApiFuture;
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

import java.util.Collection;
import java.util.HashMap;
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
        Query query = buildFilteredQuery(categoryId, status, searchTerms, null)
                .orderBy("created_at", Query.Direction.DESCENDING);

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

    /**
     * Page-number analogue of {@link #listPosts}: instead of a cursor, takes a
     * 1-indexed page and page size and returns that exact page plus the total
     * matching count (via a Firestore count aggregation, which counts
     * server-side without reading every matching document). {@code page} is
     * already validated (>= 1) by the caller.
     *
     * Firestore's {@code offset()} still costs one read per skipped document
     * server-side, so this scales worse than cursor pagination for very deep
     * pages -- acceptable here since it's what page-number UI (jump to page
     * N, show total pages) actually requires, and this app's post volumes
     * are small. If posts ever need to support very large datasets with deep
     * paging, cursor pagination (already used by CategoryPage) is the better
     * fit and this method's offset() call is the one thing to revisit.
     */
    public PageResult listPostsPage(String categoryId, String status, List<String> searchTerms, String userId,
                                     int page, int size, boolean ascending) {
        Query filtered = buildFilteredQuery(categoryId, status, searchTerms, userId);

        long totalElements = FirestoreSupport.await(filtered.count().get()).getCount();

        Query.Direction direction = ascending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
        Query sorted = filtered.orderBy("created_at", direction);

        int offset = (page - 1) * size;
        QuerySnapshot snap = FirestoreSupport.await(sorted.offset(offset).limit(size).get());
        List<PostResponse> items = snap.getDocuments().stream().map(PostResponse::fromSnapshot).toList();

        return new PageResult(items, page, size, totalElements);
    }

    private Query buildFilteredQuery(String categoryId, String status, List<String> searchTerms, String userId) {
        Query query = collection().whereEqualTo("status", status);
        if (categoryId != null && !categoryId.isBlank()) {
            query = query.whereEqualTo("category_id", categoryId);
        }
        if (userId != null && !userId.isBlank()) {
            query = query.whereEqualTo("user_id", userId);
        }
        if (!searchTerms.isEmpty()) {
            query = query.whereArrayContainsAny("search_keywords", searchTerms);
        }
        return query;
    }

    public Optional<PostResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(PostResponse.fromSnapshot(doc)) : Optional.empty();
    }

    /**
     * Batch analogue of {@link #findById}: fires one Firestore get per id
     * concurrently and blocks once on the combined result. Used where
     * several unrelated posts need to be resolved for one response (e.g.
     * report targets) instead of looping {@code findById}.
     */
    public Map<String, PostResponse> findByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ApiFuture<DocumentSnapshot>> futures = ids.stream()
                .map(id -> collection().document(id).get())
                .toList();
        Map<String, PostResponse> postById = new HashMap<>();
        for (DocumentSnapshot doc : FirestoreSupport.awaitAll(futures)) {
            if (doc.exists()) {
                postById.put(doc.getId(), PostResponse.fromSnapshot(doc));
            }
        }
        return postById;
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

    public record PageResult(List<PostResponse> items, int page, int size, long totalElements) {
    }
}
