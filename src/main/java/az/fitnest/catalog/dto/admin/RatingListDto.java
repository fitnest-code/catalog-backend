package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.RatingStatus;

import java.time.LocalDateTime;

public record RatingListDto(
        Long id,
        String customerFullName,
        Integer rating,
        String comment,
        RatingStatus status,
        LocalDateTime createdAt
) {}