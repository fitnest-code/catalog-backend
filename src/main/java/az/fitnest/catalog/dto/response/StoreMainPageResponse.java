package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "Store details for the main page")
public record StoreMainPageResponse(
    @Schema(description = "Unique identifier of the store")
    Long storeId,

    @Schema(description = "Name of the store")
    String name,

    @Schema(description = "URL of the store's cover image")
    String coverImageUrl,

    @Schema(description = "City where the store is located")
    String city,

    @Schema(description = "Address text of the store")
    String addressText,

    @Schema(description = "List of discount applies to descriptions")
    List<String> discountAppliesTo,

    @Schema(description = "Social media URL of the store")
    String social,

    @Schema(description = "Indicates if the store is saved by the user")
    Boolean isSaved,

    @Schema(description = "Indicates if the store is new")
    Boolean isNew
) {}
