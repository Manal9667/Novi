package com.novi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.dto.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Returns a clean JSON 401 (in the same {@link ApiError} shape used everywhere
 * else) when an unauthenticated request hits a protected endpoint. Without this,
 * Spring Security falls back to the servlet container's default handling, which
 * surfaces as an HTML 500 page rather than a proper 401 - authentication
 * failures happen in the filter chain, before the controller, so the
 * {@code @RestControllerAdvice} never sees them.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeError(objectMapper, response, HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI());
    }

    static void writeError(ObjectMapper objectMapper, HttpServletResponse response,
                           HttpStatus status, String message, String path) throws IOException {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                List.<String>of()
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
