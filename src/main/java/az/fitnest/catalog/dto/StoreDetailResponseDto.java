package az.fitnest.catalog.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record StoreDetailResponseDto(
    Long storeId,
    String name,
    AddressDto address,
    String phone,
    String category,
    String status,
    List<StoreDiscountDto> discounts,

    StoreSocialDto social,
    List<String> images,
    Boolean isSaved,
    Boolean isNew
) {}
