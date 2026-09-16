package com.internalmarketplace.api.category;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.category.dto.CategoryResponse;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin Firestore wrapper, a direct analogue of category.service.js's collection() helpers. */
@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.CATEGORIES);
    }

    public List<CategoryResponse> listActive() {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("status", "ACTIVE").orderBy("name").get());
        return snap.getDocuments().stream().map(CategoryResponse::fromSnapshot).toList();
    }

    public Optional<CategoryResponse> findActiveByName(String name) {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("status", "ACTIVE").whereEqualTo("name", name).limit(1).get());
        return snap.getDocuments().stream().findFirst().map(CategoryResponse::fromSnapshot);
    }

    public String create(String name, String description, String createdBy) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("status", "ACTIVE");
        data.put("created_by", createdBy);
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("updated_at", FieldValue.serverTimestamp());

        DocumentReference ref = FirestoreSupport.await(collection().add(data));
        return ref.getId();
    }

    public void update(String id, Map<String, Object> patch) {
        Map<String, Object> data = new LinkedHashMap<>(patch);
        data.put("updated_at", FieldValue.serverTimestamp());
        FirestoreSupport.await(collection().document(id).update(data));
    }

    public Optional<CategoryResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(CategoryResponse.fromSnapshot(doc)) : Optional.empty();
    }
}
