package com.novi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.dto.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A lightweight, in-memory, fixed-window rate limiter for the unauthenticated
 * auth endpoints (login/register). These are the classic brute-force /
 * credential-stuffing and signup-spam targets, so they get a per-client-IP cap
 * even though the rest of the API relies on JWT auth.
 *
 * <p>In-memory means the limit is per-instance and resets on restart - enough
 * to blunt abuse for a single-node portfolio deployment. A multi-node setup
 * would move this to a shared store (e.g. Redis) behind the same interface.
 */
@Component
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMillis;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${novi.security.rate-limit.auth.max-requests:10}") int maxRequests,
            @Value("${novi.security.rate-limit.auth.window-seconds:60}") long windowSeconds
    ) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only the state-changing auth entry points; other endpoints are unaffected.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return !("/api/auth/login".equals(path) || "/api/auth/register".equals(path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String clientKey = clientIp(request);
        if (isLimitExceeded(clientKey)) {
            writeTooManyRequests(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLimitExceeded(String clientKey) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                return new Window(now);
            }
            return existing;
        });
        return window.count.incrementAndGet() > maxRequests;
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        long retryAfterSeconds = windowMillis / 1000L;
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Too many requests - please slow down and try again shortly.",
                request.getRequestURI(),
                List.of()
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private String clientIp(HttpServletRequest request) {
        // Honor a proxy's forwarded-for header when present (the app runs behind
        // nginx in Docker), otherwise fall back to the socket address.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
