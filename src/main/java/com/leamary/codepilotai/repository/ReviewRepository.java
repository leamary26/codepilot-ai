package com.leamary.codepilotai.repository;

import com.leamary.codepilotai.entity.Review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    // Existing method
    List<Review> findAllByOrderByCreatedAtDesc();

    // NEW: Get reviews belonging to a specific user
    List<Review> findAllByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // NEW: Find a review only if it belongs to the user
    Optional<Review> findByIdAndUser_Id(
            Long id,
            Long userId
    );
}
