package com.novi.service;

import java.util.Optional;

/**
 * Produces a semantic embedding for a piece of text. Kept as an interface so
 * the embedding provider (currently Google Gemini) can be swapped without
 * touching {@code BookEmbeddingService} or the retrieval pipeline.
 */
public interface EmbeddingService {
    boolean isAvailable();

    /** Empty if the provider is unavailable or the call fails - callers must handle gracefully. */
    Optional<float[]> embed(String text);
}
