package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.GymReviewAuthorDto;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.ReviewRequest;
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
public class GymReviewService {
    private final GymRepository gymRepository;
    private final ReviewRepository reviewRepository;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Transactional(readOnly = true)
    public PaginatedResponse<GymReviewDto> getReviews(Long gymId, int page, int pageSize, String sort) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Page<Review> reviewPage = reviewRepository.findByGymId(gymId, pageable(page, pageSize, sortForReviews(sort)));
        List<GymReviewDto> items = reviewPage.getContent().stream()
                .map(r -> {
                    UserResponse user = null;
                    String fullName = "";
                    String avatarUrl = null;
                    try {
                        if (r.getUserId() != null) {
                            user = userServiceGrpcClient.getUserById(r.getUserId());
                            System.out.println("[DEBUG] gRPC user response for userId=" + r.getUserId() + ": " + user);
                            if (user != null) {
                                fullName = user.getFirstName() + " " + user.getLastName();
                                avatarUrl = user.getProfileImageUrl();
                                System.out.println("[DEBUG] fullName: " + fullName + ", avatarUrl: " + avatarUrl);
                            }
                        }
                    } catch (Exception e) {
                        fullName = "User " + r.getUserId();
                        System.out.println("[DEBUG] Exception fetching user for userId=" + r.getUserId() + ": " + e.getMessage());
                    }
                    return GymMapper.toReviewDto(r, fullName, avatarUrl);
                })
                .collect(Collectors.toList());
        return PaginatedResponse.<GymReviewDto>builder()
                .items(items)
                .total(reviewPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
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
        reviewRepository.save(review);

        reviewRepository.incrementReviewCountAndRating(gymId, (double) request.rating());
    }

    @Transactional(readOnly = true)
    public Double getGymRating(Long gymId) {
        return gymRepository.findById(gymId)
                .map(gym -> gym.getRating() != null ? gym.getRating() : 0.0)
                .orElse(0.0);
    }

    private GymReviewDto toGymReviewDto(Review r) {
        return GymReviewDto.builder()
                .review_id(r.getId() != null ? r.getId().toString() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .created_at(r.getCreatedDate() != null ? r.getCreatedDate().toLocalDate() : null)
                .author(GymReviewAuthorDto.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name("User " + r.getUserId())
                        .build())
                .build();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private Sort sortForReviews(String sort) {
        if ("newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdDate");
        } else if ("highest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        } else if ("lowest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "rating");
        }
        return Sort.unsorted();
    }
}
