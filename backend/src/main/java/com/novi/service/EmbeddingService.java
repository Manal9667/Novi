package com.novi.service;

import java.util.Optional;

/**
 * Produces a semantic embedding for a piece of text. Kept as an interface so
 * the embedding provider (currently Voyage AI) can be swapped without
 * touching {@code BookEmbeddingService} or {@code TasteProfileService}.
 */
public interface EmbeddingService {
    boolean isAvailable();

    /** Empty if the provider is unavailable or the call fails - callers must handle gracefully. */
    Optional<float[]> embed(String text);
}
