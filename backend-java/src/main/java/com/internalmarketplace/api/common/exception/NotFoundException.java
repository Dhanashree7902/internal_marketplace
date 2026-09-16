package com.internalmarketplace.api.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    public NotFoundException(String publicMessage) {
        super(HttpStatus.NOT_FOUND, publicMessage);
    }
}
