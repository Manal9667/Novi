package com.novi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a clean JSON 403 when an authenticated user lacks permission for a
 * resource, mirroring {@link RestAuthenticationEntryPoint}. Like authentication
 * failures, authorization failures occur in the filter chain and never reach
 * the {@code @RestControllerAdvice}.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RestAuthenticationEntryPoint.writeError(
                objectMapper, response, HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }
}
