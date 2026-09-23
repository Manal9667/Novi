package com.novi.eval;

import java.util.List;

/**
 * Runs a {@link Retriever} over a set of {@link LabeledQuery labeled queries}
 * and aggregates retrieval-quality metrics ({@link RetrievalMetrics}) at a
 * cutoff {@code k}. This is the reusable core of both the offline
 * embedding-representation experiment and any future evaluation of the live
 * pgvector retriever.
 */
public class RetrievalEvaluationHarness {

    public MetricsReport evaluate(Retriever retriever, List<LabeledQuery> queries, int k) {
        if (queries.isEmpty()) {
            return new MetricsReport(k, 0, 0, 0, 0, 0);
        }

        double sumPrecision = 0, sumRecall = 0, sumRr = 0, sumNdcg = 0;
        for (LabeledQuery query : queries) {
            List<String> ranked = retriever.retrieve(query.vector(), k);
            sumPrecision += RetrievalMetrics.precisionAtK(ranked, query.relevantDocIds(), k);
            sumRecall += RetrievalMetrics.recallAtK(ranked, query.relevantDocIds(), k);
            sumRr += RetrievalMetrics.reciprocalRank(ranked, query.relevantDocIds());
            sumNdcg += RetrievalMetrics.ndcgAtK(ranked, query.relevantDocIds(), k);
        }

        int n = queries.size();
        return new MetricsReport(k, n, sumPrecision / n, sumRecall / n, sumRr / n, sumNdcg / n);
    }
}
