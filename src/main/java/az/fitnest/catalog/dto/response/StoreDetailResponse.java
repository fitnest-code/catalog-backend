package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import java.util.List;

@Builder
public record StoreDetailResponse(
    Long storeId,
    String name,
    AddressResponse address,
    String phone,
    String category,
    String status,
    List<StoreDiscountResponse> discounts,

    StoreSocialDto social,
    List<String> images,
    Boolean isSaved,
    Boolean isNew
) {}
