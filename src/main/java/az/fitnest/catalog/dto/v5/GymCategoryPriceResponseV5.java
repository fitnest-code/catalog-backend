package az.fitnest.catalog.dto.v5;

import java.time.LocalDate;

public record GymCategoryPriceResponseV5(
        Long categoryId,
        Long packageId,
        String packageName,
        Double singlePrice,
        Double unlimitedPrice,
        LocalDate effectiveFrom) {}
