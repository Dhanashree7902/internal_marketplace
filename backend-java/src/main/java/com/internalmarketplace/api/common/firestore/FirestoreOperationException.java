package com.internalmarketplace.api.common.firestore;

import com.google.cloud.firestore.FirestoreException;
import io.grpc.Status;

/** Unchecked wrapper around a failed Firestore {@code ApiFuture}. */
public class FirestoreOperationException extends RuntimeException {

    public FirestoreOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * True when this failure is Firestore's ALREADY_EXISTS status — the
     * expected outcome of losing a race on {@code DocumentReference.create()}
     * (mirrors the try/catch around userRef.create(...) in the Node
     * middleware/auth.js first-sign-in auto-provision path).
     */
    public boolean isAlreadyExists() {
        Throwable cause = getCause();
        return cause instanceof FirestoreException firestoreException
                && firestoreException.getStatus() != null
                && firestoreException.getStatus().getCode() == Status.Code.ALREADY_EXISTS;
    }
}
