package az.fitnest.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Search-page filter chip. HOME_ELIGIBLE is served first and carries translation
 * keys, real categories carry their id/name/icon.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchCategoryResponse(
        String type,
        String id,
        String name,
        String iconUrl,
        String titleKey,
        String subtitleKey,
        long gymCount
) {
}
