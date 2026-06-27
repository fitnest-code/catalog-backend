package az.fitnest.catalog.dto.response;

public record GymDescriptionResponse(
    Long categoryId,
    String categoryName,
    String phone,
    String description,
    String coverImageUrl
) {}
