package com.novi.service;

import com.novi.dto.user.UserProfileResponse;
import com.novi.entity.User;
import com.novi.entity.enums.ReadingStatus;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.ReviewRepository;
import com.novi.repository.UserBookRepository;
import com.novi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBookRepository userBookRepository;
    private final ReviewRepository reviewRepository;

    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' not found"));

        long read = userBookRepository.findByUserAndStatus(user, ReadingStatus.READ).size();
        long reading = userBookRepository.findByUserAndStatus(user, ReadingStatus.CURRENTLY_READING).size();
        long wantToRead = userBookRepository.findByUserAndStatus(user, ReadingStatus.WANT_TO_READ).size();
        long reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user).size();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getCreatedAt(),
                read,
                reading,
                wantToRead,
                reviews
        );
    }
}
