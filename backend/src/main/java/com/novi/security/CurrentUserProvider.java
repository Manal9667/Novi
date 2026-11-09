package com.novi.security;

import com.novi.entity.User;
import com.novi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** Resolves the currently authenticated User entity from the security context. */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return user;
    }

    /**
     * Resolves the authenticated user, or {@code null} when the request is
     * anonymous. Used by endpoints that are public but personalize when a user
     * is signed in (e.g. natural-language recommendations).
     */
    public User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails principal)) {
            return null;
        }
        return userRepository.findByUsername(principal.getUsername()).orElse(null);
    }
}
