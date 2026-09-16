package com.internalmarketplace.api.common.firestore;

/** Firestore collection names, matching the strings used throughout backend/src/modules. */
public final class FirestoreCollections {

    public static final String USERS = "users";
    public static final String CATEGORIES = "categories";
    public static final String CATEGORY_REQUESTS = "categoryRequests";
    public static final String POSTS = "posts";
    public static final String POST_IMAGES_SUBCOLLECTION = "images";
    public static final String COMMENTS = "comments";
    public static final String REPORTS = "reports";
    public static final String NOTIFICATIONS = "notifications";
    public static final String AUDIT_LOGS = "auditLogs";
    public static final String ROLE_REMOVAL_REQUESTS = "roleRemovalRequests";

    private FirestoreCollections() {
    }
}
