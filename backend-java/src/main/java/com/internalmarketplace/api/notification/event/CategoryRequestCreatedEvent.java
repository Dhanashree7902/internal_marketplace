package com.internalmarketplace.api.notification.event;

/** Published after a category request is persisted; replaces functions/src/index.js's onCategoryRequestCreate. */
public record CategoryRequestCreatedEvent(String requestId, String requestedBy, String proposedName) {
}
