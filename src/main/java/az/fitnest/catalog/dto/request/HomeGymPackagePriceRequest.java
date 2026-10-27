package az.fitnest.catalog.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Versioned home-gym price for one package of a gym (spec R15/R17).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeGymPackagePriceRequest {

    private Long packageId;

    /**
     * Whether the package is part of the home-gym offer. Defaults to true.
     */
    private Boolean included;

    /**
     * Optional package-level switch. When null the gym master switch applies.
     */
    private Boolean homeEligible;

    private Double singlePrice;

    private Double unlimitedPrice;

    /**
     * First day of the month the price applies from. Defaults to the first day of next month
     * (R17); non-first-of-month values are normalized to the first day of that month.
     */
    private LocalDate effectiveFrom;
}
