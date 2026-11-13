
package com.leamary.codepilotai;

import com.leamary.codepilotai.controller.HistoryController;
import com.leamary.codepilotai.dto.ReviewResponse;
import com.leamary.codepilotai.entity.Review;
import com.leamary.codepilotai.entity.User;
import com.leamary.codepilotai.repository.ReviewRepository;
import com.leamary.codepilotai.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistoryControllerTest {

    private ReviewRepository reviewRepository;
    private UserRepository userRepository;
    private HistoryController historyController;
    private Authentication authentication;

    private User currentUser;
    private Review sampleReview;

    @BeforeEach
    void setUp() {

        reviewRepository = mock(ReviewRepository.class);
        userRepository = mock(UserRepository.class);
        authentication = mock(Authentication.class);

        historyController = new HistoryController(
                reviewRepository,
                userRepository
        );

        currentUser = new User(
                "Sample User",
                "sample@codepilot.test",
                "sample-password-hash"
        );

        currentUser.setId(1L);

        when(authentication.getName())
                .thenReturn(currentUser.getEmail());

        when(userRepository.findByEmail(
                currentUser.getEmail()
        )).thenReturn(Optional.of(currentUser));

        sampleReview = new Review(
                "Java",
                "int a = 10;",
                "Sample AI review",
                80,
                currentUser
        );

        sampleReview.setId(10L);
    }

    @Test
    void shouldReturnOnlyCurrentUsersReviews() {

        when(reviewRepository
                .findAllByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(sampleReview));

        List<ReviewResponse> reviews =
                historyController.getHistory(authentication);

        assertEquals(1, reviews.size());
        assertEquals(10L, reviews.get(0).getId());
        assertEquals("Java", reviews.get(0).getLanguage());
        assertEquals(80, reviews.get(0).getQualityScore());

        verify(reviewRepository)
                .findAllByUser_IdOrderByCreatedAtDesc(1L);
    }

    @Test
    void shouldReturnEmptyHistoryForNewUser() {

        when(reviewRepository
                .findAllByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<ReviewResponse> reviews =
                historyController.getHistory(authentication);

        assertTrue(reviews.isEmpty());
    }

    @Test
    void shouldAllowViewingOwnReview() {

        when(reviewRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(sampleReview));

        ReviewResponse response =
                historyController.getReview(
                        10L, authentication
                );

        assertEquals(10L, response.getId());
        assertEquals("Sample AI review", response.getReview());
    }

    @Test
    void shouldRejectViewingAnotherUsersReview() {

        when(reviewRepository.findByIdAndUser_Id(20L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> historyController.getReview(
                        20L, authentication
                )
        );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void shouldAllowDeletingOwnReview() {

        when(reviewRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(sampleReview));

        historyController.deleteReview(
                10L, authentication
        );

        verify(reviewRepository).delete(sampleReview);
    }

    @Test
    void shouldRejectDeletingAnotherUsersReview() {

        when(reviewRepository.findByIdAndUser_Id(20L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> historyController.deleteReview(
                        20L, authentication
                )
        );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        verify(reviewRepository, never())
                .delete(any(Review.class));
    }
}

