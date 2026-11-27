package az.fitnest.catalog.dto.v5;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GymEntranceScanRequestV5(
        @NotBlank String qrCodeValue,
        Double lat,
        Double lng,
        String confirmation,
        String confirmationToken,
        String qrScanId,
        /** Required when gym has subcategories (or multiple offered categories). */
        @Positive Long categoryId) {}
