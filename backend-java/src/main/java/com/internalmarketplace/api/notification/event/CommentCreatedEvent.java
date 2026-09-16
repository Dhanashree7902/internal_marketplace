package com.internalmarketplace.api.notification.event;

/** Published after a comment is persisted; replaces functions/src/index.js's onCommentCreate. */
public record CommentCreatedEvent(String commentId, String postId, String authorUid) {
}
