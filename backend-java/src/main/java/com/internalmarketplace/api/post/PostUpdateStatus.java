package com.internalmarketplace.api.post;

/** Statuses an owner may set via PATCH /posts/{id} (distinct from admin moderation statuses). */
public enum PostUpdateStatus {
    ACTIVE, CLOSED, SOLD
}
