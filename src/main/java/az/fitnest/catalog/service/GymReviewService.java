package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.GymReviewResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.request.ReviewRequest;

public interface GymReviewService {
    PaginatedResponse<GymReviewResponse> getReviews(Long gymId, int page, int pageSize, String sort);
    void addReview(Long userId, Long gymId, ReviewRequest request);
    Long approveReview(Long reviewId);
    void rejectReview(Long reviewId);
    PaginatedResponse<GymReviewResponse> getPendingReviews(int page, int pageSize);
    Double getGymRating(Long gymId);
}
