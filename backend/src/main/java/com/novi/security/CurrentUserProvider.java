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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        String username = ((UserDetails) auth.getPrincipal()).getUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
    }
}
