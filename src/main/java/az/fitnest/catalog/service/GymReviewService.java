package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.ReviewRequest;

public interface GymReviewService {
    PaginatedResponse<GymReviewDto> getReviews(Long gymId, int page, int pageSize, String sort);
    void addReview(Long userId, Long gymId, ReviewRequest request);
    Long approveReview(Long reviewId);
    void rejectReview(Long reviewId);
    PaginatedResponse<GymReviewDto> getPendingReviews(int page, int pageSize);
    Double getGymRating(Long gymId);
}
