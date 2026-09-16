package com.internalmarketplace.api.common.search;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Poor-man's full-text search for Firestore (FR-12), a direct port of
 * lib/searchKeywords.js: tokenizes title + description + category name into
 * a lowercase word array stored on the post doc, queried later with
 * array-contains-any. Matches whole words only, not substrings.
 */
public final class SearchKeywords {

    private static final int MAX_BUILD_KEYWORDS = 200;
    private static final int MAX_QUERY_TERMS = 10; // Firestore's array-contains-any limit
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z0-9]+");

    private SearchKeywords() {
    }

    public static List<String> buildSearchKeywords(String title, String description, String categoryName) {
        String text = String.join(" ",
                title == null ? "" : title,
                description == null ? "" : description,
                categoryName == null ? "" : categoryName);
        return tokenize(text, MAX_BUILD_KEYWORDS);
    }

    public static List<String> normalizeSearchQuery(String query) {
        return tokenize(query == null ? "" : query, MAX_QUERY_TERMS);
    }

    private static List<String> tokenize(String text, int limit) {
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        LinkedHashSet<String> words = new LinkedHashSet<>();
        while (matcher.find() && words.size() < limit) {
            words.add(matcher.group());
        }
        return List.copyOf(words);
    }
}
