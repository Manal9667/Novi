package com.novi.eval;

/**
 * Aggregate retrieval-quality metrics over an evaluation set, averaged across
 * all queries at cutoff {@code k}.
 */
public record MetricsReport(
        int k,
        int queryCount,
        double meanPrecision,
        double meanRecall,
        double meanReciprocalRank,
        double meanNdcg
) {
    public String toTableRow(String label) {
        return String.format("%-18s  P@%d=%.3f  R@%d=%.3f  MRR=%.3f  nDCG@%d=%.3f",
                label, k, meanPrecision, k, meanRecall, meanReciprocalRank, k, meanNdcg);
    }
}
