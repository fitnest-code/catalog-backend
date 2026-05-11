package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author: nijataghayev
 */

@Data
public class DiscountItemRequest {

    @NotNull
    private Long packageId;

    @NotNull
    @Min(1) @Max(100)
    private Integer discountPercent;
}
