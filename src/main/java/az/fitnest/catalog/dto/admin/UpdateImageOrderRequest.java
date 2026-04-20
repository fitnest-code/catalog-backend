package az.fitnest.catalog.dto.admin;

import java.util.List;

public record UpdateImageOrderRequest(
        List<ImageOrderItem> images
) {
    public record ImageOrderItem(Long id, Integer sortOrder) {
    }
}
