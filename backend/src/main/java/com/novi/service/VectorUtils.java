package com.novi.service;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Small helpers for serializing embeddings to/from the JSON-text columns and computing similarity. */
public final class VectorUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VectorUtils() {}

    public static String toJson(float[] vector) {
        try {
            return MAPPER.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize embedding vector", e);
        }
    }

    public static float[] fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, float[].class);
        } catch (Exception e) {
            return null;
        }
    }

    public static double cosineSimilarity(float[] a, float[] b) {
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

    /** Element-wise weighted average of several vectors of the same dimension. */
    public static float[] weightedAverage(java.util.List<float[]> vectors, java.util.List<Double> weights) {
        if (vectors.isEmpty()) return null;
        // Determine dimension from the first non-null vector rather than blindly
        // dereferencing element 0, which could be null.
        int dim = -1;
        for (float[] v : vectors) {
            if (v != null) {
                dim = v.length;
                break;
            }
        }
        if (dim <= 0) return null;
        double[] sum = new double[dim];
        double totalWeight = 0;
        for (int i = 0; i < vectors.size(); i++) {
            float[] v = vectors.get(i);
            double w = weights.get(i);
            if (v == null || v.length != dim) continue;
            for (int j = 0; j < dim; j++) {
                sum[j] += v[j] * w;
            }
            totalWeight += w;
        }
        if (totalWeight == 0) return null;
        float[] result = new float[dim];
        for (int j = 0; j < dim; j++) {
            result[j] = (float) (sum[j] / totalWeight);
        }
        return result;
    }
}
