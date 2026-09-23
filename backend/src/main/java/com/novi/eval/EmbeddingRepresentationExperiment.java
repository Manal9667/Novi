package com.novi.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Compares embedding-representation / similarity strategies on the same labeled
 * dataset and selects the one with the best retrieval quality (by nDCG@K). This
 * is the "embedding optimization, evaluated with semantic-similarity metrics"
 * step: rather than assuming raw cosine is best, it measures each option and
 * picks the winner from data.
 */
public class EmbeddingRepresentationExperiment {

    public record StrategyResult(VectorRetriever.ScoringStrategy strategy, MetricsReport report) {}

    public record ExperimentOutcome(List<StrategyResult> results, StrategyResult best) {}

    private final RetrievalEvaluationHarness harness = new RetrievalEvaluationHarness();

    public ExperimentOutcome run(Map<String, float[]> corpus, List<LabeledQuery> queries, int k) {
        List<StrategyResult> results = new ArrayList<>();
        for (VectorRetriever.ScoringStrategy strategy : VectorRetriever.ScoringStrategy.values()) {
            VectorRetriever retriever = new VectorRetriever(corpus, strategy);
            results.add(new StrategyResult(strategy, harness.evaluate(retriever, queries, k)));
        }
        StrategyResult best = results.stream()
                .max(Comparator.comparingDouble(r -> r.report().meanNdcg()))
                .orElseThrow();
        return new ExperimentOutcome(results, best);
    }
}
