package com.internalmarketplace.api.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException(String publicMessage) {
        super(HttpStatus.CONFLICT, publicMessage);
    }
}
