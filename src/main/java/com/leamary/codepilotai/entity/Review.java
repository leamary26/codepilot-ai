package com.leamary.codepilotai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String language;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String code;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String review;

    private Integer qualityScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // NEW: Connect each review to its owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Default constructor
    public Review() {
    }

    // Existing constructor
    public Review(
            String language,
            String code,
            String review,
            Integer qualityScore) {

        this.language = language;
        this.code = code;
        this.review = review;
        this.qualityScore = qualityScore;
        this.createdAt = LocalDateTime.now();
    }

    // NEW: Constructor with user ownership
    public Review(
            String language,
            String code,
            String review,
            Integer qualityScore,
            User user) {

        this.language = language;
        this.code = code;
        this.review = review;
        this.qualityScore = qualityScore;
        this.createdAt = LocalDateTime.now();
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // NEW: User getter and setter
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
