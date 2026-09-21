package com.novi.service;

/**
 * Small, dependency-free string-similarity helper used by the book scanner to
 * score how closely the text a vision model read off a spine matches a real
 * catalog book's title/author. Returns a normalized 0.0-1.0 ratio based on
 * Levenshtein edit distance over case-/whitespace-normalized strings.
 */
public final class TextSimilarity {

    private TextSimilarity() {
    }

    /** 1.0 == identical (after normalization), 0.0 == completely different. */
    public static double ratio(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() && nb.isEmpty()) {
            return 1.0;
        }
        if (na.isEmpty() || nb.isEmpty()) {
            return 0.0;
        }
        if (na.equals(nb)) {
            return 1.0;
        }
        int distance = levenshtein(na, nb);
        int longest = Math.max(na.length(), nb.length());
        return 1.0 - ((double) distance / longest);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        // Lower-case, strip punctuation, collapse whitespace - so "The Hobbit"
        // and "the hobbit." are treated as equal.
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
