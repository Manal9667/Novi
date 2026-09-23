package com.novi.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalMetricsTest {

    // Ranked: [a, b, c, d]; relevant: {a, c}
    private final List<String> ranked = List.of("a", "b", "c", "d");
    private final Set<String> relevant = Set.of("a", "c");

    @Test
    void precisionAtK_countsHitsInTopKOverK() {
        assertThat(RetrievalMetrics.precisionAtK(ranked, relevant, 2)).isEqualTo(0.5);   // {a} hit of 2 -> 1/2
        assertThat(RetrievalMetrics.precisionAtK(ranked, relevant, 4)).isEqualTo(0.5);   // {a,c} of 4 -> 2/4
    }

    @Test
    void recallAtK_countsHitsOverTotalRelevant() {
        assertThat(RetrievalMetrics.recallAtK(ranked, relevant, 1)).isEqualTo(0.5);      // found a of {a,c}
        assertThat(RetrievalMetrics.recallAtK(ranked, relevant, 3)).isEqualTo(1.0);      // found a and c
    }

    @Test
    void reciprocalRank_usesFirstRelevantRank() {
        assertThat(RetrievalMetrics.reciprocalRank(ranked, relevant)).isEqualTo(1.0);    // a at rank 1
        assertThat(RetrievalMetrics.reciprocalRank(List.of("b", "c", "a"), relevant)).isEqualTo(0.5); // c at rank 2
        assertThat(RetrievalMetrics.reciprocalRank(List.of("x", "y"), relevant)).isEqualTo(0.0);
    }

    @Test
    void ndcgAtK_isOneForIdealOrderingAndLessOtherwise() {
        // Ideal: both relevant first.
        assertThat(RetrievalMetrics.ndcgAtK(List.of("a", "c", "b", "d"), relevant, 4)).isEqualTo(1.0);

        // ranked = [a,b,c,d]: DCG = 1/log2(2) + 1/log2(4) = 1 + 0.5 = 1.5
        // IDCG    = 1/log2(2) + 1/log2(3) = 1 + 0.6309 = 1.6309
        double ndcg = RetrievalMetrics.ndcgAtK(ranked, relevant, 4);
        assertThat(ndcg).isCloseTo(1.5 / 1.6309, org.assertj.core.data.Offset.offset(1e-3));
        assertThat(ndcg).isLessThan(1.0);
    }

    @Test
    void emptyRelevantOrZeroK_yieldZero() {
        assertThat(RetrievalMetrics.precisionAtK(ranked, relevant, 0)).isZero();
        assertThat(RetrievalMetrics.recallAtK(ranked, Set.of(), 4)).isZero();
        assertThat(RetrievalMetrics.ndcgAtK(ranked, Set.of(), 4)).isZero();
    }
}
