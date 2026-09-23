package com.novi.eval;

import java.util.Set;

/**
 * A single evaluation query: an embedding vector plus the set of document ids
 * that are known to be relevant to it (the ground truth). Used by
 * {@link RetrievalEvaluationHarness} to score a retriever's ranked output.
 */
public record LabeledQuery(String id, float[] vector, Set<String> relevantDocIds) {}
