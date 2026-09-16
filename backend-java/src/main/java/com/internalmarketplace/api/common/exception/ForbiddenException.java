package com.internalmarketplace.api.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String publicMessage) {
        super(HttpStatus.FORBIDDEN, publicMessage);
    }
}
