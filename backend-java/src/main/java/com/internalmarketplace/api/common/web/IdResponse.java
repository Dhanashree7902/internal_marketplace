package com.internalmarketplace.api.common.web;

/** Matches every {@code res.status(201).json({ id })} response in the Node controllers. */
public record IdResponse(String id) {
}
