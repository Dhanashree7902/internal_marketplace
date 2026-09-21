package com.internalmarketplace.api.common.firestore;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;

import java.util.List;
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

    /**
     * Blocks on a batch of independent Firestore futures at once instead of
     * one at a time. Each {@link ApiFuture} is already in flight the moment
     * it's created (the Firestore client issues the gRPC call immediately
     * and hands back a future, it doesn't wait for a caller to block on it),
     * so calling this on a list built with {@code collection().document(id).get()}
     * per id turns N sequential round trips into one round trip whose latency
     * is that of the slowest single lookup, not the sum of all of them.
     */
    public static <T> List<T> awaitAll(List<ApiFuture<T>> futures) {
        return await(ApiFutures.allAsList(futures));
    }
}
