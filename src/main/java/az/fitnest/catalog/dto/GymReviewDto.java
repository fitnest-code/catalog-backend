package az.fitnest.catalog.dto;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record GymReviewDto(
    String review_id,
    Integer rating,
    String comment,
    GymReviewAuthorDto author,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDate created_at
) {}
