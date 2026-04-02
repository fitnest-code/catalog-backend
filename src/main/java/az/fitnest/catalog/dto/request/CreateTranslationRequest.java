package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create or update a translation")
public record CreateTranslationRequest(
    @Schema(description = "Type of the entity", example = "Category")
    String entityType,
    @Schema(description = "ID of the entity", example = "1")
    String entityId,
    @Schema(description = "Language code", example = "AZ")
    String languageCode,
    @Schema(description = "Name of the field", example = "name")
    String fieldName,
    @Schema(description = "Translated value", example = "Geyim")
    String fieldValue
) {}
