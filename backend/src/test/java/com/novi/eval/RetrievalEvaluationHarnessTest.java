package com.novi.eval;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalEvaluationHarnessTest {

    private final RetrievalEvaluationHarness harness = new RetrievalEvaluationHarness();

    @Test
    void cosineRetriever_ranksBySimilarityAndScoresPerfectlyOnAlignedQuery() {
        Map<String, float[]> corpus = new LinkedHashMap<>();
        corpus.put("a", new float[] {1f, 0f});
        corpus.put("b", new float[] {0f, 1f});
        corpus.put("c", new float[] {0.9f, 0.1f});

        // Query aligned with a and c; b is orthogonal.
        LabeledQuery query = new LabeledQuery("q", new float[] {1f, 0f}, Set.of("a", "c"));

        VectorRetriever retriever = new VectorRetriever(corpus, VectorRetriever.ScoringStrategy.COSINE);
        assertThat(retriever.retrieve(query.vector(), 3)).containsExactly("a", "c", "b");

        MetricsReport report = harness.evaluate(retriever, List.of(query), 2);
        assertThat(report.meanPrecision()).isCloseTo(1.0, Offset.offset(1e-9));
        assertThat(report.meanRecall()).isCloseTo(1.0, Offset.offset(1e-9));
        assertThat(report.meanReciprocalRank()).isCloseTo(1.0, Offset.offset(1e-9));
        assertThat(report.meanNdcg()).isCloseTo(1.0, Offset.offset(1e-9));
    }

    @Test
    void experimentIsReproducibleAndProducesValidMetrics() {
        var first = RetrievalEvaluationRunner.runDefaultExperiment();
        var second = RetrievalEvaluationRunner.runDefaultExperiment();

        // Reproducible: same seed -> identical winner and identical nDCG.
        assertThat(first.best().strategy()).isEqualTo(second.best().strategy());
        assertThat(first.best().report().meanNdcg())
                .isEqualTo(second.best().report().meanNdcg());

        // Every strategy yields metrics in the valid [0,1] range over all queries.
        assertThat(first.results()).hasSize(VectorRetriever.ScoringStrategy.values().length);
        for (var r : first.results()) {
            assertThat(r.report().meanPrecision()).isBetween(0.0, 1.0);
            assertThat(r.report().meanRecall()).isBetween(0.0, 1.0);
            assertThat(r.report().meanNdcg()).isBetween(0.0, 1.0);
            assertThat(r.report().queryCount())
                    .isEqualTo(RetrievalEvaluationRunner.NUM_TOPICS * RetrievalEvaluationRunner.QUERIES_PER_TOPIC);
        }

        // Retrieval genuinely works on clustered data: the best strategy is well
        // above random ranking.
        assertThat(first.best().report().meanNdcg()).isGreaterThan(0.5);
    }
}
