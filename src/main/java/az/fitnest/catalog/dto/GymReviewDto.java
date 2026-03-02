package az.fitnest.catalog.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record GymReviewDto(
    String review_id,
    Integer rating,
    String comment,
    GymReviewAuthorDto author,
    LocalDateTime created_at
) {}
