package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record GymReviewResponse(
    Long id,
    String review_id,
    Integer rating,
    String comment,
    GymReviewAuthorResponse author,
    String status,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate created_at
) {}
