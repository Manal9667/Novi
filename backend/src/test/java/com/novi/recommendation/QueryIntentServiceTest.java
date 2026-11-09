package com.novi.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.service.LlmClient;
import com.novi.service.QueryIntent;
import com.novi.service.QueryIntentService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryIntentServiceTest {

    private final LlmClient llm = mock(LlmClient.class);
    private final QueryIntentService service = new QueryIntentService(llm, new ObjectMapper());

    @Test
    void parsesStructuredIntentFromLlmJson() {
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString(), anyInt())).thenReturn(Optional.of("""
                {"genres":["fantasy"],"themes":["magic"],"culturalContext":[],"authors":[],
                 "similarTo":[],"moods":["dark"],"keywords":["dragons"],"length":null}
                """));

        QueryIntent intent = service.extract("a dark fantasy about dragons and magic");

        assertThat(intent.genres()).contains("fantasy");
        assertThat(intent.themes()).contains("magic");
        assertThat(intent.retrievalTerms()).contains("fantasy", "magic", "dragons");
    }

    @Test
    void fallsBackToKeywordsWhenLlmUnavailable() {
        when(llm.isAvailable()).thenReturn(false);

        QueryIntent intent = service.extract("psychological thriller mystery");

        assertThat(intent.keywords()).contains("psychological", "thriller", "mystery");
    }

    @Test
    void fallsBackToKeywordsOnMalformedJson() {
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString(), anyInt())).thenReturn(Optional.of("not valid json at all"));

        QueryIntent intent = service.extract("historical fiction china");

        assertThat(intent.keywords()).contains("historical", "fiction", "china");
    }
}
