package az.fitnest.catalog.dto;
import lombok.Builder;
@Builder
public record GymCategoryCountResponse(String categoryName, long count) {}
