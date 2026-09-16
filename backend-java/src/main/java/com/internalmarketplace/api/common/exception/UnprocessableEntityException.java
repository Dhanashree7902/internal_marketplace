package com.internalmarketplace.api.common.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends ApiException {
    public UnprocessableEntityException(String publicMessage) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, publicMessage);
    }
}
