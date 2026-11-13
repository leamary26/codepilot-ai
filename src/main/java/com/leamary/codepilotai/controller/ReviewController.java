package com.leamary.codepilotai.controller;

import com.leamary.codepilotai.dto.ReviewRequest;
import com.leamary.codepilotai.service.ReviewService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService) {

        this.reviewService = reviewService;
    }

    // Public health check
    @GetMapping("/health")
    public String health() {

        return "CodePilot AI Backend is running!";
    }

    // Review code for authenticated user
    @PostMapping("/review")
    public String reviewCode(
            @RequestBody ReviewRequest request,
            Authentication authentication) {

        String email =
                authentication.getName();

        return reviewService.reviewCode(
                request,
                email
        );
    }
}
