package com.internalmarketplace.api.common.firestore;

import com.google.api.core.ApiFuture;

import java.util.concurrent.ExecutionException;

/**
 * Blocks on a Firestore {@link ApiFuture}, the Java-SDK analogue of a Node
 * {@code await db.collection(...).get()} call. Safe to call from any request
 * thread because virtual threads (enabled in application.yml) park cheaply
 * while the future completes, instead of pinning a platform thread.
 */
public final class FirestoreSupport {

    private FirestoreSupport() {
    }

    public static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreOperationException("Interrupted while waiting on Firestore", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new FirestoreOperationException(cause.getMessage(), cause);
        }
    }
}
