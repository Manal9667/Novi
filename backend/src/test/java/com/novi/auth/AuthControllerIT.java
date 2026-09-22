package com.novi.auth;

import com.novi.dto.auth.LoginRequest;
import com.novi.dto.auth.RegisterRequest;
import com.novi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    void registerThenLogin_succeeds() {
        RegisterRequest register = new RegisterRequest("reader1", "Reader One", "password123", "password123");

        ResponseEntity<Object> registerResponse =
                restTemplate.postForEntity("/api/auth/register", register, Object.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest login = new LoginRequest("reader1", "password123");
        ResponseEntity<Object> loginResponse = restTemplate.postForEntity("/api/auth/login", login, Object.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void register_withDuplicateUsername_returnsConflict() {
        RegisterRequest register = new RegisterRequest("reader2", "Reader Two", "password123", "password123");
        restTemplate.postForEntity("/api/auth/register", register, Object.class);

        ResponseEntity<Object> secondAttempt =
                restTemplate.postForEntity("/api/auth/register", register, Object.class);

        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_withMismatchedPasswords_returnsBadRequest() {
        RegisterRequest register = new RegisterRequest("reader3", "Reader Three", "password123", "different456");

        ResponseEntity<Object> response = restTemplate.postForEntity("/api/auth/register", register, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() {
        RegisterRequest register = new RegisterRequest("reader4", "Reader Four", "password123", "password123");
        restTemplate.postForEntity("/api/auth/register", register, Object.class);

        LoginRequest badLogin = new LoginRequest("reader4", "wrongPassword");
        ResponseEntity<Object> response = restTemplate.postForEntity("/api/auth/login", badLogin, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/api/library", Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
