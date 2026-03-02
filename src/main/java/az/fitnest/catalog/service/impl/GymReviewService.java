package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.GymReviewAuthorDto;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.ReviewRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Review;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ReviewRepository;
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

    @Transactional(readOnly = true)
    public PaginatedResponse<GymReviewDto> getReviews(Long gymId, int page, int pageSize, String sort) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "İdman zalı tapılmadı");
        }
        Page<Review> reviewPage = reviewRepository.findByGymId(gymId, pageable(page, pageSize, sortForReviews(sort)));
        List<GymReviewDto> items = reviewPage.getContent().stream()
                .map(this::toGymReviewDto)
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
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "İdman zalı tapılmadı");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setGymId(gymId);
        review.setRating(request.rating());
        review.setComment(request.comment());
        reviewRepository.save(review);

        // Atomic update of rating and reviewsCount avoids concurrency race condition bugs
        reviewRepository.incrementReviewCountAndRating(gymId, (double) request.rating());
    }

    private GymReviewDto toGymReviewDto(Review r) {
        return GymReviewDto.builder()
                .review_id(r.getId() != null ? r.getId().toString() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .created_at(r.getCreatedDate())
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
