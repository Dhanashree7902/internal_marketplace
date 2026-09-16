package com.internalmarketplace.api.common.web;

/** Matches the Node controllers' {@code res.json({ data })} envelope. */
public record DataResponse<T>(T data) {
    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data);
    }
}
