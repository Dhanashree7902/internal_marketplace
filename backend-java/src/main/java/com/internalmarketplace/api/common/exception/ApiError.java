package com.internalmarketplace.api.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Matches the Node error handler's {@code { error, details? } } response shape exactly. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, List<FieldIssue> details) {

    public static ApiError of(String error) {
        return new ApiError(error, null);
    }

    public static ApiError of(String error, List<FieldIssue> details) {
        return new ApiError(error, details);
    }

    public record FieldIssue(String field, String message) {
    }
}
