package com.internalmarketplace.api.user;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import com.internalmarketplace.api.user.dto.UserSchemaMigrationResult;
import com.internalmarketplace.api.user.dto.UserSchemaMigrationResult.UpdatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One-time backfill that brings every existing users/{uid} document up to the
 * schema now enforced for newly auto-provisioned records (see
 * FirebaseAuthenticationFilter): created_at, email, employee_id, name, role,
 * status, updated_at all present, and the deprecated department field gone.
 *
 * Only fills gaps and removes department -- it never overwrites an existing
 * non-blank value, so a legacy record's real data (including a genuine,
 * already-unique employee_id) is left untouched. Runs in bounded pages so a
 * large collection doesn't get pulled into memory at once or blow past
 * Firestore's 500-write batch limit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSchemaMigrationService {

    private static final int PAGE_SIZE = 300;

    private final Firestore firestore;
    private final FirebaseAuth firebaseAuth;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.USERS);
    }

    public UserSchemaMigrationResult migrate(boolean dryRun) {
        int scanned = 0;
        List<UpdatedUser> updated = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        Set<String> seenEmployeeIds = new HashSet<>();

        Query baseQuery = collection().orderBy(FieldPath.documentId()).limit(PAGE_SIZE);
        QueryDocumentSnapshot lastDoc = null;

        while (true) {
            Query page = lastDoc == null ? baseQuery : baseQuery.startAfter(lastDoc);
            List<QueryDocumentSnapshot> docs = FirestoreSupport.await(page.get()).getDocuments();
            if (docs.isEmpty()) {
                break;
            }

            WriteBatch batch = firestore.batch();
            boolean hasWrites = false;

            for (QueryDocumentSnapshot doc : docs) {
                scanned++;
                Map<String, Object> patch = new LinkedHashMap<>();

                resolveEmployeeId(doc, patch, seenEmployeeIds, conflicts);
                resolveEmailAndName(doc, patch, unresolved);

                if (isBlank(doc.getString("role"))) {
                    patch.put("role", "employee");
                }
                if (isBlank(doc.getString("status"))) {
                    patch.put("status", "ACTIVE");
                }
                if (doc.getTimestamp("created_at") == null) {
                    patch.put("created_at", FieldValue.serverTimestamp());
                }
                if (doc.contains("department")) {
                    patch.put("department", FieldValue.delete());
                }

                if (!patch.isEmpty()) {
                    patch.put("updated_at", FieldValue.serverTimestamp());
                    updated.add(new UpdatedUser(doc.getId(), List.copyOf(patch.keySet())));
                    if (!dryRun) {
                        batch.update(doc.getReference(), patch);
                        hasWrites = true;
                    }
                }
            }

            if (!dryRun && hasWrites) {
                FirestoreSupport.await(batch.commit());
            }

            lastDoc = docs.get(docs.size() - 1);
            if (docs.size() < PAGE_SIZE) {
                break;
            }
        }

        log.info("User schema migration ({}): scanned={}, updated={}, employeeIdConflicts={}, unresolved={}",
                dryRun ? "dry-run" : "applied", scanned, updated.size(), conflicts.size(), unresolved.size());

        return new UserSchemaMigrationResult(dryRun, scanned, updated.size(), updated, conflicts, unresolved);
    }

    // employee_id must be non-blank and unique. Missing -> generate the same
    // "EMP-{uid}" id new sign-ins get (unique by construction, see
    // FirebaseAuthenticationFilter). Already present -> leave it alone, but
    // flag it if it collides with another record's employee_id so an admin
    // can resolve which one is correct instead of the migration guessing.
    private void resolveEmployeeId(QueryDocumentSnapshot doc, Map<String, Object> patch,
                                    Set<String> seenEmployeeIds, List<String> conflicts) {
        String existing = doc.getString("employee_id");
        if (!isBlank(existing)) {
            if (!seenEmployeeIds.add(existing)) {
                conflicts.add(doc.getId() + " (duplicate employee_id: " + existing + ")");
            }
            return;
        }
        String generated = "EMP-" + doc.getId();
        seenEmployeeIds.add(generated);
        patch.put("employee_id", generated);
    }

    // Legacy records (e.g. created via scripts/createUser.js) may only ever have had
    // role/status stored. Backfill from the matching Firebase Auth account -- the same
    // source FirebaseAuthenticationFilter already falls back to live on every request
    // for these accounts -- so the fallback is persisted once instead of recomputed forever.
    private void resolveEmailAndName(QueryDocumentSnapshot doc, Map<String, Object> patch, List<String> unresolved) {
        String existingEmail = doc.getString("email");
        String existingName = doc.getString("name");
        boolean needsEmail = isBlank(existingEmail);
        boolean needsName = isBlank(existingName);
        if (!needsEmail && !needsName) {
            return;
        }

        String authEmail = null;
        String authName = null;
        boolean authLookupFailed = false;
        try {
            UserRecord authRecord = firebaseAuth.getUser(doc.getId());
            authEmail = authRecord.getEmail();
            authName = authRecord.getDisplayName();
        } catch (FirebaseAuthException e) {
            authLookupFailed = true;
            unresolved.add(doc.getId() + " (no matching Firebase Auth account: " + e.getMessage() + ")");
        }

        String resolvedEmail = !isBlank(existingEmail) ? existingEmail : authEmail;
        if (needsEmail) {
            if (!isBlank(resolvedEmail)) {
                patch.put("email", resolvedEmail);
            } else if (!authLookupFailed) {
                unresolved.add(doc.getId() + " (no email available in Firebase Auth either)");
            }
        }

        String resolvedName = !isBlank(authName) ? authName : resolvedEmail;
        if (needsName) {
            if (!isBlank(resolvedName)) {
                patch.put("name", resolvedName);
            } else if (!authLookupFailed) {
                unresolved.add(doc.getId() + " (no name or email available to derive one)");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
