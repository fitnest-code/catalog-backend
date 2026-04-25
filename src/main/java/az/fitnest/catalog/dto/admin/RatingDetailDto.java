package az.fitnest.catalog.dto.admin;

import az.fitnest.catalog.model.enums.RatingStatus;

import java.time.LocalDateTime;

public record RatingDetailDto(
        Long id,
        Long customerId,
        String customerFullName,
        Integer rating,
        String comment,
        RatingStatus status,
        String moderationNote,
        Long moderatedBy,
        LocalDateTime moderatedAt,
        LocalDateTime createdAt
) {}
