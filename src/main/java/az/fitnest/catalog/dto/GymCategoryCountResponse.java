package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymCategoryCountResponse(Long categoryId, String categoryName, long count) {}
