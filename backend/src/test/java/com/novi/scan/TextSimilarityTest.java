package com.novi.scan;

import com.novi.service.TextSimilarity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSimilarityTest {

    @Test
    void identicalStrings_scoreOne() {
        assertThat(TextSimilarity.ratio("The Hobbit", "The Hobbit")).isEqualTo(1.0);
    }

    @Test
    void ignoresCasePunctuationAndWhitespace() {
        assertThat(TextSimilarity.ratio("The Hobbit", "the   hobbit.")).isEqualTo(1.0);
    }

    @Test
    void closeButNotExact_scoresHigh() {
        double score = TextSimilarity.ratio("The Great Gatsby", "Great Gatsby");
        assertThat(score).isBetween(0.7, 0.99);
    }

    @Test
    void completelyDifferent_scoresLow() {
        double score = TextSimilarity.ratio("Dune", "Pride and Prejudice");
        assertThat(score).isLessThan(0.4);
    }

    @Test
    void bothEmpty_scoreOne_butOneEmpty_scoreZero() {
        assertThat(TextSimilarity.ratio("", "")).isEqualTo(1.0);
        assertThat(TextSimilarity.ratio("Dune", "")).isEqualTo(0.0);
    }
}
