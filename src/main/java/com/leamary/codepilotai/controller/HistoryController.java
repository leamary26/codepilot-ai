
package com.leamary.codepilotai.controller;

import com.leamary.codepilotai.dto.ReviewResponse;
import com.leamary.codepilotai.entity.Review;
import com.leamary.codepilotai.entity.User;
import com.leamary.codepilotai.repository.ReviewRepository;
import com.leamary.codepilotai.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public HistoryController(
            ReviewRepository reviewRepository,
            UserRepository userRepository) {

        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    // Find the currently authenticated user
    private User getCurrentUser(
            Authentication authentication) {

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "User not found."
                        )
                );
    }

    // 1. Get only the current user's reviews
    @GetMapping
    public List<ReviewResponse> getHistory(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return reviewRepository
                .findAllByUser_IdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(ReviewResponse::new)
                .toList();
    }

    // 2. View one review belonging to the user
    @GetMapping("/{id}")
    public ReviewResponse getReview(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Review review = reviewRepository
                .findByIdAndUser_Id(
                        id,
                        user.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Review not found."
                        )
                );

        return new ReviewResponse(review);
    }

    // 3. Delete only the current user's review
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Review review = reviewRepository
                .findByIdAndUser_Id(
                        id,
                        user.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Review not found."
                        )
                );

        reviewRepository.delete(review);
    }
}
