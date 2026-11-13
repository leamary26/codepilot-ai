
package com.leamary.codepilotai;

import com.leamary.codepilotai.dto.ReviewRequest;
import com.leamary.codepilotai.repository.ReviewRepository;
import com.leamary.codepilotai.repository.UserRepository;
import com.leamary.codepilotai.service.ReviewService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private UserRepository userRepository;
    private ReviewService reviewService;

    private static final String EMAIL =
            "demo@codepilot.test";

    @BeforeEach
    void setUp() {

        reviewRepository =
                mock(ReviewRepository.class);

        userRepository =
                mock(UserRepository.class);

        reviewService = new ReviewService(
                reviewRepository,
                userRepository
        );
    }

    // Helper method to create a review request

    private ReviewRequest createRequest(
            String code,
            String language) {

        ReviewRequest request =
                new ReviewRequest();

        request.setCode(code);
        request.setLanguage(language);

        return request;
    }

    // Test 1: Reject missing request

    @Test
    void shouldRejectNullRequest() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                null,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Review request is required.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 2: Reject empty code

    @Test
    void shouldRejectEmptyCode() {

        ReviewRequest request =
                createRequest("", "Java");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Please provide code to review.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 3: Reject whitespace-only code

    @Test
    void shouldRejectBlankCode() {

        ReviewRequest request =
                createRequest("   \n\t  ", "Java");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Please provide code to review.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 4: Reject null code

    @Test
    void shouldRejectNullCode() {

        ReviewRequest request =
                createRequest(null, "Java");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Please provide code to review.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 5: Reject code exceeding 20,000 characters

    @Test
    void shouldRejectOversizedCode() {

        String largeCode = "a".repeat(20001);

        ReviewRequest request =
                createRequest(largeCode, "Java");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Code must not exceed 20000 characters.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 6: Reject missing programming language

    @Test
    void shouldRejectMissingLanguage() {

        ReviewRequest request =
                createRequest("print(123)", null);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Please select a programming language.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 7: Reject unsupported programming language

    @Test
    void shouldRejectUnsupportedLanguage() {

        ReviewRequest request =
                createRequest(
                        "print(123)",
                        "InvalidLanguage"
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Unsupported programming language.",
                exception.getReason()
        );

        verifyNoInteractions(
                reviewRepository,
                userRepository
        );
    }

    // Test 8: Accept the exact maximum code length

    @Test
    void shouldAcceptMaximumCodeLength() {

        String maximumCode = "a".repeat(20000);

        ReviewRequest request =
                createRequest(maximumCode, "Java");

        // Stop at user lookup to avoid calling Gemini

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reviewService.reviewCode(
                                request,
                                EMAIL
                        )
                );

        // 401 proves the request passed input validation
        // and reached the user lookup stage

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        assertEquals(
                "User not found.",
                exception.getReason()
        );

        verify(userRepository)
                .findByEmail(EMAIL);

        verifyNoInteractions(reviewRepository);
    }
}
