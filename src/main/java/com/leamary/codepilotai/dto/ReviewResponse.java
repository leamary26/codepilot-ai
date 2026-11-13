
package com.leamary.codepilotai.dto;

import com.leamary.codepilotai.entity.Review;
import java.time.LocalDateTime;

public class ReviewResponse {

    private Long id;
    private String language;
    private String code;
    private String review;
    private Integer qualityScore;
    private LocalDateTime createdAt;

    public ReviewResponse(Review savedReview) {

        this.id = savedReview.getId();
        this.language = savedReview.getLanguage();
        this.code = savedReview.getCode();
        this.review = savedReview.getReview();
        this.qualityScore = savedReview.getQualityScore();
        this.createdAt = savedReview.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getCode() {
        return code;
    }

    public String getReview() {
        return review;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

