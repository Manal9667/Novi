package com.novi.service;

import com.novi.dto.auth.AuthResponse;
import com.novi.dto.auth.LoginRequest;
import com.novi.dto.auth.RegisterRequest;
import com.novi.entity.User;
import com.novi.exception.BadRequestException;
import com.novi.exception.DuplicateResourceException;
import com.novi.repository.UserRepository;
import com.novi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }

        User user = User.builder()
                .username(request.username())
                .displayName(request.displayName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getDisplayName());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getDisplayName());
    }
}
