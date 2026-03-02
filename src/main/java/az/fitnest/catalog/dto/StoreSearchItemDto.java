package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record StoreSearchItemDto(
    Long storeId
) {}
