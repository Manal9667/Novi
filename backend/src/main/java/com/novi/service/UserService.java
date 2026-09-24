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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBookRepository userBookRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' not found"));

        long read = userBookRepository.countByUserAndStatus(user, ReadingStatus.READ);
        long reading = userBookRepository.countByUserAndStatus(user, ReadingStatus.CURRENTLY_READING);
        long wantToRead = userBookRepository.countByUserAndStatus(user, ReadingStatus.WANT_TO_READ);
        long reviews = reviewRepository.countByUser(user);

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
