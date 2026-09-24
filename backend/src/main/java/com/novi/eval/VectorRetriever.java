package com.novi.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * In-memory vector retriever used for offline retrieval-quality experiments.
 * Ranks a fixed corpus of embeddings against a query vector using one of
 * several scoring strategies, so an experiment can measure how much the choice
 * of similarity / embedding post-processing changes retrieval quality:
 *
 * <ul>
 *   <li>{@link ScoringStrategy#DOT_PRODUCT} - raw inner product (magnitude-sensitive)</li>
 *   <li>{@link ScoringStrategy#COSINE} - L2-normalized similarity (magnitude-invariant)</li>
 *   <li>{@link ScoringStrategy#CENTERED_COSINE} - subtract the corpus mean before
 *       cosine; a common embedding post-processing step ("all-but-the-mean")
 *       that removes the shared component and often sharpens semantic ranking</li>
 * </ul>
 */
public class VectorRetriever implements Retriever {

    public enum ScoringStrategy { DOT_PRODUCT, COSINE, CENTERED_COSINE }

    private final List<Map.Entry<String, float[]>> corpus;
    private final ScoringStrategy strategy;
    private final float[] mean; // null unless CENTERED_COSINE

    public VectorRetriever(Map<String, float[]> corpus, ScoringStrategy strategy) {
        this.corpus = new ArrayList<>(corpus.entrySet());
        this.strategy = strategy;
        this.mean = strategy == ScoringStrategy.CENTERED_COSINE ? computeMean(corpus.values()) : null;
    }

    @Override
    public List<String> retrieve(float[] queryVector, int k) {
        float[] q = transform(queryVector);
        return corpus.stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, float[]> e) -> score(q, transform(e.getValue()))).reversed())
                .limit(Math.max(0, k))
                .map(Map.Entry::getKey)
                .toList();
    }

    private double score(float[] q, float[] d) {
        return strategy == ScoringStrategy.DOT_PRODUCT ? dot(q, d) : cosine(q, d);
    }

    private static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] transform(float[] v) {
        if (mean == null || v == null) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] - mean[i];
        return out;
    }

    private static double dot(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    private static float[] computeMean(Iterable<float[]> vectors) {
        float[] mean = null;
        int count = 0;
        for (float[] v : vectors) {
            if (v == null) continue;
            if (mean == null) mean = new float[v.length];
            if (v.length != mean.length) continue;
            for (int i = 0; i < v.length; i++) mean[i] += v[i];
            count++;
        }
        if (mean == null || count == 0) return new float[0];
        for (int i = 0; i < mean.length; i++) mean[i] /= count;
        return mean;
    }
}
