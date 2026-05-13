package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Paket endirimi elementi")
public class DiscountItemRequest {

    @NotNull
    @Schema(description = "Paketin unikal identifikatoru", example = "1")
    private Long packageId;

    @NotNull
    @Min(value = 1,   message = "Endirim faizi minimum 1 olmalıdır")
    @Max(value = 100, message = "Endirim faizi maksimum 100 olmalıdır")
    @Schema(description = "Endirim faizi (1-100 arasında)", example = "15")
    private Integer discountPercent;
}
