package com.novi.eval;

import java.util.List;
import java.util.Set;

/**
 * Standard information-retrieval quality metrics over a ranked list of document
 * ids and a set of known-relevant ids (binary relevance). Pure functions with
 * no dependencies, so they are trivially unit-testable and reusable for both
 * offline experiments and any future online evaluation.
 *
 * <ul>
 *   <li>Precision@K  - fraction of the top-K results that are relevant</li>
 *   <li>Recall@K     - fraction of all relevant docs found in the top K</li>
 *   <li>Reciprocal rank - 1/rank of the first relevant result (mean over
 *       queries gives MRR)</li>
 *   <li>nDCG@K        - rank-discounted gain, normalized by the ideal ordering</li>
 * </ul>
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {}

    public static double precisionAtK(List<String> ranked, Set<String> relevant, int k) {
        if (k <= 0 || ranked.isEmpty()) return 0.0;
        int limit = Math.min(k, ranked.size());
        int hits = 0;
        for (int i = 0; i < limit; i++) {
            if (relevant.contains(ranked.get(i))) hits++;
        }
        return (double) hits / k;
    }

    public static double recallAtK(List<String> ranked, Set<String> relevant, int k) {
        if (k <= 0 || relevant.isEmpty()) return 0.0;
        int limit = Math.min(k, ranked.size());
        int hits = 0;
        for (int i = 0; i < limit; i++) {
            if (relevant.contains(ranked.get(i))) hits++;
        }
        return (double) hits / relevant.size();
    }

    /** 1/rank of the first relevant result (rank is 1-based); 0 if none in the list. */
    public static double reciprocalRank(List<String> ranked, Set<String> relevant) {
        for (int i = 0; i < ranked.size(); i++) {
            if (relevant.contains(ranked.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /** Discounted cumulative gain over the top K, with binary gains. */
    public static double dcgAtK(List<String> ranked, Set<String> relevant, int k) {
        if (k <= 0) return 0.0;
        int limit = Math.min(k, ranked.size());
        double dcg = 0.0;
        for (int i = 0; i < limit; i++) {
            if (relevant.contains(ranked.get(i))) {
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2)); // 1/log2(i+2)
            }
        }
        return dcg;
    }

    /** nDCG@K = DCG@K divided by the ideal DCG@K (all relevant docs ranked first). */
    public static double ndcgAtK(List<String> ranked, Set<String> relevant, int k) {
        if (k <= 0 || relevant.isEmpty()) return 0.0;
        double dcg = dcgAtK(ranked, relevant, k);
        int idealHits = Math.min(k, relevant.size());
        double idcg = 0.0;
        for (int i = 0; i < idealHits; i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }
        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }
}
