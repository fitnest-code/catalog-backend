package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Request to create or update a store")
public record StoreRequest(
    @NotBlank
    @Schema(description = "Store name", example = "Fit Market")
    String name,

    @Schema(description = "Store address")
    AddressDto address,

    @Schema(description = "Store phone")
    String phone,

    @Schema(description = "Store category", example = "SUPPLEMENTS")
    String category,

    @Schema(description = "Store status", example = "ACTIVE")
    String status,

    @Schema(description = "Store social media links")
    StoreSocialDto social
) {}
