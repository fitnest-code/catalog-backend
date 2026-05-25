package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.GymReviewAuthorResponse;
import az.fitnest.catalog.dto.response.GymReviewResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.request.ReviewRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.GymMapper;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.Review;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReviewRepository;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymReviewServiceImpl implements az.fitnest.catalog.service.GymReviewService {
    private final GymRepository gymRepository;
    private final ReviewRepository reviewRepository;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Transactional(readOnly = true)
    public PaginatedResponse<GymReviewResponse> getReviews(Long gymId, int page, int pageSize, String sort) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Page<Review> reviewPage = reviewRepository.findByGymId(gymId, pageable(page, pageSize, sortForReviews(sort)));
        List<GymReviewResponse> items = reviewPage.getContent().stream()
                .map(this::mapReviewToDto)
                .collect(Collectors.toList());
        return PaginatedResponse.<GymReviewResponse>builder()
                .items(items)
                .total(reviewPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<GymReviewResponse> getGymReviewsAdmin(Long gymId, az.fitnest.catalog.model.enums.ReviewStatus status, String search, int page, int pageSize, String sort) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }

        Pageable pageable = pageable(page, pageSize, sortForReviews(sort));
        Page<Review> reviewPage = reviewRepository.findByGymIdAndStatusAndSearch(gymId, status, search, pageable);

        List<GymReviewResponse> items = reviewPage.getContent().stream()
                .map(this::mapReviewToDto)
                .collect(Collectors.toList());

        return PaginatedResponse.<GymReviewResponse>builder()
                .items(items)
                .total(reviewPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GymReviewResponse getReviewDetail(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("REVIEW_NOT_FOUND", "error.review_not_found"));
        return mapReviewToDto(review);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", allEntries = true)
    public void addReview(Long userId, Long gymId, ReviewRequest request) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Gym gym = gymRepository.findById(gymId).orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Review review = new Review();
        review.setUserId(userId);
        review.setGymId(gymId);
        review.setGym(gym);
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setStatus(az.fitnest.catalog.model.enums.ReviewStatus.PENDING);
        reviewRepository.save(review);
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", allEntries = true)
    public Long approveReview(Long reviewId) {
        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new ResourceNotFoundException("REVIEW_NOT_FOUND", "error.review_not_found"));

            if (review.getStatus() != az.fitnest.catalog.model.enums.ReviewStatus.ACCEPTED) {
                reviewRepository.updateStatus(reviewId, az.fitnest.catalog.model.enums.ReviewStatus.ACCEPTED);

                if (review.getRating() != null) {
                    reviewRepository.incrementReviewCountAndRating(review.getGymId(), review.getRating().doubleValue());
                }
            }
            return review.getGymId();
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "gym-detail", allEntries = true)
    public void rejectReview(Long reviewId) {
        try {
            if (!reviewRepository.existsById(reviewId)) {
                throw new ResourceNotFoundException("REVIEW_NOT_FOUND", "error.review_not_found");
            }
            reviewRepository.updateStatus(reviewId, az.fitnest.catalog.model.enums.ReviewStatus.REJECTED);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<GymReviewResponse> getPendingReviews(int page, int pageSize) {
        Page<Review> reviewPage = reviewRepository.findByStatus(az.fitnest.catalog.model.enums.ReviewStatus.PENDING, pageable(page, pageSize, Sort.by(Sort.Direction.DESC, "createdDate")));
        List<GymReviewResponse> items = reviewPage.getContent().stream()
                .map(this::mapReviewToDto)
                .collect(Collectors.toList());
        return PaginatedResponse.<GymReviewResponse>builder()
                .items(items)
                .total(reviewPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private GymReviewResponse mapReviewToDto(Review r) {
        UserResponse user = null;
        String fullName = "";
        String avatarUrl = null;
        try {
            if (r.getUserId() != null) {
                user = userServiceGrpcClient.getUserById(r.getUserId());
                if (user != null) {
                    fullName = user.getFirstName() + " " + user.getLastName();
                    avatarUrl = user.getProfileImageUrl();
                }
            }
        } catch (Exception e) {
            fullName = "User " + r.getUserId();
        }
        return GymMapper.toReviewDto(r, fullName, avatarUrl);
    }

    @Transactional(readOnly = true)
    public Double getGymRating(Long gymId) {
        return gymRepository.findById(gymId)
                .map(gym -> gym.getRating() != null ? gym.getRating() : 0.0)
                .orElse(0.0);
    }

    private GymReviewResponse toGymReviewDto(Review r) {
        return new GymReviewResponse(
                r.getId(),
                r.getId() != null ? r.getId().toString() : null,
                r.getRating(),
                r.getComment(),
                GymReviewAuthorResponse.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name("User " + r.getUserId())
                        .build(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCreatedDate() != null ? r.getCreatedDate().toLocalDate() : null
        );
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private Sort sortForReviews(String sort) {
        if ("newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdDate");
        } else if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdDate");
        } else if ("highest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        } else if ("lowest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "rating");
        } else if ("gym_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "gym.name");
        } else if ("gym_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "gym.name");
        }
        return Sort.by(Sort.Direction.DESC, "createdDate");
    }
}
