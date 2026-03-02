package az.fitnest.catalog.dto;

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
    Double distanceKm,
    StoreSocialDto social,
    Boolean isSaved,
    Boolean isNew
) {}
