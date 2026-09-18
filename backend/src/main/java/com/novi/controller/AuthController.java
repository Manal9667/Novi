package com.novi.controller;

import com.novi.dto.auth.AuthResponse;
import com.novi.dto.auth.LoginRequest;
import com.novi.dto.auth.RegisterRequest;
import com.novi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWTs: logout is a client-side action (discard the token).
        // Endpoint kept for a clean frontend contract / future token-blacklisting.
        return ResponseEntity.noContent().build();
    }
}
