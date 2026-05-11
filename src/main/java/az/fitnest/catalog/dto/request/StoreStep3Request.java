package az.fitnest.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author: nijataghayev
 */

@Data
public class StoreStep3Request {

    @NotEmpty
    @Valid
    private List<DiscountItemRequest> discounts;
}
