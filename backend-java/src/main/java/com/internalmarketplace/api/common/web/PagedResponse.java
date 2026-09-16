package com.internalmarketplace.api.common.web;

import java.util.List;

/** Matches PostController's {@code res.json({ data, nextCursor })} envelope. */
public record PagedResponse<T>(List<T> data, String nextCursor) {
}
