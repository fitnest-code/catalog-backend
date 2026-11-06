package az.fitnest.catalog.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record SearchCategoriesResponse(
        List<SearchCategoryResponse> categories
) {
}
