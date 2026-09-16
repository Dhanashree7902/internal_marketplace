package com.internalmarketplace.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for every business-rule failure, mirroring the {@code err.status} /
 * {@code err.publicMessage} convention used throughout the Node services
 * (e.g. category.service.js, post.service.js).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String publicMessage;

    public ApiException(HttpStatus status, String publicMessage) {
        super(publicMessage);
        this.status = status;
        this.publicMessage = publicMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getPublicMessage() {
        return publicMessage;
    }
}
