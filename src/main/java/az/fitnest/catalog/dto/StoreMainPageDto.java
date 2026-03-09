package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Store details for the main page")
public record StoreMainPageDto(
    @Schema(description = "Unique identifier of the store")
    Long storeId,

    @Schema(description = "Name of the store")
    String name,

    @Schema(description = "Phone number of the store")
    String phone,

    @Schema(description = "Address of the store")
    String address,

    @Schema(description = "City where the store is located")
    String city,

    @Schema(description = "URL of the store's logo")
    String logoUrl,

    @Schema(description = "URL of the store's cover image")
    String coverImageUrl,

    @Schema(description = "List of discounts available at the store")
    java.util.List<StoreDiscountDto> discounts,

    @Schema(description = "Distance to the store in kilometers")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Double distanceKm,

    @Schema(description = "Social media links of the store")
    StoreSocialDto social,

    @Schema(description = "Indicates if the store is saved by the user")
    Boolean isSaved,

    @Schema(description = "Indicates if the store is new")
    Boolean isNew
) {}
