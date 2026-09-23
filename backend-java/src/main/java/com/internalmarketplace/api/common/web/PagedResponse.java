package com.internalmarketplace.api.common.web;

import java.util.List;

/**
 * Two pagination modes share this one envelope: cursor-based (only {@code data}
 * and {@code nextCursor} populated -- the original shape, still used by
 * CategoryPage's "Load More") and page-number-based (the {@code page}/{@code size}/
 * {@code totalElements}/{@code totalPages}/{@code hasNext}/{@code hasPrevious} fields
 * populated instead -- used by Home and My Posts). Whichever mode wasn't used
 * for a given response leaves its fields {@code null}, which the app-wide
 * {@code non_null} Jackson setting drops from the JSON body, so neither mode's
 * consumers see the other mode's fields at all.
 */
public record PagedResponse<T>(
        List<T> data,
        String nextCursor,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        Boolean hasNext,
        Boolean hasPrevious
) {
    public static <T> PagedResponse<T> cursor(List<T> data, String nextCursor) {
        return new PagedResponse<>(data, nextCursor, null, null, null, null, null, null);
    }

    public static <T> PagedResponse<T> page(List<T> data, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil(totalElements / (double) size);
        return new PagedResponse<>(data, null, page, size, totalElements, totalPages, page < totalPages, page > 1);
    }
}
