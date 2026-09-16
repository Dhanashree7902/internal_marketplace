package com.internalmarketplace.api.moderation;

/** Admin moderation outcome for a post (distinct from the owner-scoped PostUpdateStatus). */
public enum PostModerationStatus {
    REMOVED, ACTIVE, REJECTED
}
