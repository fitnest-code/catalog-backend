package az.fitnest.catalog.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record StoreSearchResponseDto(
    List<StoreSearchItemDto> items,
    long total,
    int page,
    int pageSize
) {}
