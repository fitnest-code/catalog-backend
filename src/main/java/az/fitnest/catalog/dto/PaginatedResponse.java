package az.fitnest.catalog.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;
import java.util.List;

@Builder
public record PaginatedResponse<T>(
    List<T> items,
    long total,
    int page,
    int pageSize,
    String message
) {
    public static <T> PaginatedResponse<T> of(Page<T> pageResult) {
        int pageNumber = pageResult.getNumber() + 1;
        return PaginatedResponse.<T>builder()
                .items(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .page(pageNumber)
                .pageSize(pageResult.getSize())
                .build();
    }
}
