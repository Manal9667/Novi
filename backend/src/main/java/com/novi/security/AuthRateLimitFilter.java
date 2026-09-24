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

    /**
     * Hard cap on how many distinct client keys we track at once. Bounds memory
     * even under a flood of never-before-seen keys (e.g. a spoofed or rotating
     * source address); once reached we evict expired windows and, if still full,
     * reset the map. See {@link #pruneIfNecessary(long)}.
     */
    private static final int MAX_TRACKED_CLIENTS = 100_000;

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMillis;
    private final boolean trustForwardedFor;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${novi.security.rate-limit.auth.max-requests:10}") int maxRequests,
            @Value("${novi.security.rate-limit.auth.window-seconds:60}") long windowSeconds,
            @Value("${novi.security.rate-limit.auth.trust-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        this.trustForwardedFor = trustForwardedFor;
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
        pruneIfNecessary(now);
        Window window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                return new Window(now);
            }
            return existing;
        });
        return window.count.incrementAndGet() > maxRequests;
    }

    /**
     * Keeps the tracking map bounded. Only runs work once the map is large, so
     * the common path stays O(1): first drop every window that has fully
     * expired, then - if a flood of still-active keys keeps us at the cap -
     * reset entirely rather than grow without limit. Resetting can briefly let
     * an in-flight abuser start a fresh window, which is an acceptable trade for
     * a hard memory bound on a single-node, in-memory limiter.
     */
    private void pruneIfNecessary(long now) {
        if (windows.size() < MAX_TRACKED_CLIENTS) {
            return;
        }
        windows.entrySet().removeIf(e -> now - e.getValue().windowStart >= windowMillis);
        if (windows.size() >= MAX_TRACKED_CLIENTS) {
            windows.clear();
        }
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
        // X-Forwarded-For is client-supplied and trivially spoofable, so a caller
        // could otherwise send a new value per request to get a fresh bucket
        // every time (defeating the limit) and grow the tracking map without
        // bound. Only honor it when explicitly told we sit behind a trusted proxy
        // that overwrites the header (novi.security.rate-limit.auth.trust-forwarded-for=true).
        // Otherwise key on the real socket address.
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
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
