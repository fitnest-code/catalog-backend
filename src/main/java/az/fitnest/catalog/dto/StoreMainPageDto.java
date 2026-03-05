package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
public record StoreMainPageDto(
    Long storeId,
    String name,
    String phone,
    String address,
    String city,
    String logoUrl,
    String coverImageUrl,
    java.util.List<StoreDiscountDto> discounts,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Double distanceKm,
    StoreSocialDto social,
    Boolean isSaved,
    Boolean isNew
) {}
