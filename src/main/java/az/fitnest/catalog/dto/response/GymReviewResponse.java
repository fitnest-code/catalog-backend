package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record GymReviewResponse(
    String review_id,
    Integer rating,
    String comment,
    GymReviewAuthorResponse author,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDate created_at
) {}
