package com.novi.book;

import com.novi.dto.auth.AuthResponse;
import com.novi.dto.auth.RegisterRequest;
import com.novi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Web-layer tests for the public book endpoints and the admin-only embedding
 * backfill. Complements the service-level unit tests by exercising routing,
 * the response envelope, and the security rules end-to-end.
 */
class BookControllerIT extends AbstractIntegrationTest {

    @Test
    void browse_returnsPaginatedEnvelope() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/books", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // The consistent PageResponse shape: a content array plus paging metadata.
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"totalElements\"");
    }

    @Test
    void backfillEmbeddings_withoutToken_returnsUnauthorized() {
        ResponseEntity<Object> response =
                restTemplate.postForEntity("/api/books/backfill-embeddings", null, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void backfillEmbeddings_asNonAdmin_returnsForbidden() {
        String token = registerAndGetToken("plain_reader", "Plain Reader");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/books/backfill-embeddings", HttpMethod.POST, new HttpEntity<>(headers), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void backfillEmbeddings_asAdmin_isAuthorized() {
        // "admin_user" is configured as an admin in application-test.yml.
        String token = registerAndGetToken("admin_user", "Admin User");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/books/backfill-embeddings", HttpMethod.POST, new HttpEntity<>(headers), Object.class);

        // Authorized: not blocked by security. Without embedding providers configured
        // the backfill is a no-op, but the request must get past the ADMIN check.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String registerAndGetToken(String username, String displayName) {
        RegisterRequest register = new RegisterRequest(username, displayName, "password123", "password123");
        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().token();
    }
}
