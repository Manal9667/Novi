package com.novi.eval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deterministically generates a labeled retrieval dataset for reproducible
 * experiments: {@code numTopics} latent topics, each a random centroid in
 * {@code dim}-dimensional space; {@code docsPerTopic} documents scattered around
 * each centroid; and {@code queriesPerTopic} queries per topic whose relevant
 * set is exactly the documents drawn from the same topic.
 *
 * <p>Everything is driven by a single seed, so the same seed always yields the
 * same corpus, queries and ground truth - a reproducible data-processing
 * workflow for experimentation and evaluation.
 */
public final class SyntheticEvaluationData {

    public record Dataset(Map<String, float[]> corpus, List<LabeledQuery> queries) {}

    private SyntheticEvaluationData() {}

    public static Dataset generate(long seed, int numTopics, int docsPerTopic,
                                   int queriesPerTopic, int dim, double noise) {
        Random random = new Random(seed);

        List<float[]> centroids = new ArrayList<>();
        for (int t = 0; t < numTopics; t++) {
            centroids.add(randomUnitVector(random, dim));
        }

        Map<String, float[]> corpus = new LinkedHashMap<>();
        List<Set<String>> docsByTopic = new ArrayList<>();
        for (int t = 0; t < numTopics; t++) {
            Set<String> topicDocs = new HashSet<>();
            for (int d = 0; d < docsPerTopic; d++) {
                String docId = "t" + t + "_d" + d;
                corpus.put(docId, jitter(centroids.get(t), random, noise));
                topicDocs.add(docId);
            }
            docsByTopic.add(topicDocs);
        }

        List<LabeledQuery> queries = new ArrayList<>();
        for (int t = 0; t < numTopics; t++) {
            for (int q = 0; q < queriesPerTopic; q++) {
                queries.add(new LabeledQuery(
                        "t" + t + "_q" + q,
                        jitter(centroids.get(t), random, noise),
                        docsByTopic.get(t)));
            }
        }

        return new Dataset(corpus, queries);
    }

    private static float[] randomUnitVector(Random random, int dim) {
        float[] v = new float[dim];
        double norm = 0;
        for (int i = 0; i < dim; i++) {
            v[i] = (float) random.nextGaussian();
            norm += v[i] * v[i];
        }
        norm = Math.sqrt(norm);
        if (norm == 0) norm = 1;
        for (int i = 0; i < dim; i++) v[i] /= norm;
        return v;
    }

    /** Centroid + Gaussian noise, then a random positive magnitude so raw dot
     *  product and cosine diverge (magnitude carries no topic signal). */
    private static float[] jitter(float[] centroid, Random random, double noise) {
        float[] v = new float[centroid.length];
        for (int i = 0; i < centroid.length; i++) {
            v[i] = (float) (centroid[i] + random.nextGaussian() * noise);
        }
        float magnitude = (float) (0.5 + random.nextDouble() * 2.0);
        for (int i = 0; i < v.length; i++) v[i] *= magnitude;
        return v;
    }
}
