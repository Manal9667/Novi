package com.novi.controller;

import com.novi.dto.user.UserProfileResponse;
import com.novi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{username}")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userService.getProfile(username);
    }
}
