package az.fitnest.catalog.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record StoreListResponseDto(
    List<StoreMainPageDto> stores,
    long total,
    int page,
    int pageSize
) {}
