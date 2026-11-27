package az.fitnest.catalog.dto.v5;

import java.time.LocalDate;

/** Versioned price of one package for a gym category (same semantics as HomeGymPackagePriceRequest). */
public record GymCategoryPriceRequestV5(
        Long packageId, Double singlePrice, Double unlimitedPrice, LocalDate effectiveFrom) {}
