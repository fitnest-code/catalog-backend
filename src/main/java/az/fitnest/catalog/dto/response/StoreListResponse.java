package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import java.util.List;

@Builder
public record StoreListResponse(
    List<StoreMainPageResponse> stores,
    long total,
    int page,
    int pageSize
) {}
