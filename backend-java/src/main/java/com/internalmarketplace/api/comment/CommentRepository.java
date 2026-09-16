package com.internalmarketplace.api.comment;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.comment.dto.CommentResponse;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thin Firestore wrapper, a direct analogue of comments.service.js. */
@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.COMMENTS);
    }

    public List<CommentResponse> listByPost(String postId) {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("post_id", postId).orderBy("created_at").get());
        return snap.getDocuments().stream().map(CommentResponse::fromSnapshot).toList();
    }

    public String create(String postId, String userId, String userEmail, String text) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("post_id", postId);
        data.put("user_id", userId);
        data.put("user_email", userEmail);
        data.put("text", text);
        data.put("status", "ACTIVE");
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("updated_at", FieldValue.serverTimestamp());

        DocumentReference ref = FirestoreSupport.await(collection().add(data));
        return ref.getId();
    }
}
