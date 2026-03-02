package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymReviewAuthorDto(
    String user_id,
    String full_name,
    String avatar_url
) {}
