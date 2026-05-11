package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author: nijataghayev
 */

@Data
@Schema(description = "Mağaza yaradılması - Addım 3: Paket endirimləri və aktivləşdirmə")
public class StoreStep3Request {

    @NotEmpty(message = "Endirim siyahısı boş ola bilməz")
    @Valid
    @Schema(description = "Paket endirimləri siyahısı")
    private List<DiscountItemRequest> discounts;
}
