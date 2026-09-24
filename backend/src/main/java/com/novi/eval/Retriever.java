package com.novi.eval;

import java.util.List;

/**
 * Abstraction over "given a query vector, return the top-K document ids, best
 * first". Lets the evaluation harness score any retrieval strategy - an
 * in-memory vector retriever for offline experiments, or a live
 * pgvector-backed retriever - through the same interface.
 */
@FunctionalInterface
public interface Retriever {
    List<String> retrieve(float[] queryVector, int k);
}
