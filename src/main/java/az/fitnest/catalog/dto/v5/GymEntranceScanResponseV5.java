package az.fitnest.catalog.dto.v5;

import az.fitnest.catalog.dto.response.GymEntranceScanResponse;

public record GymEntranceScanResponseV5(
        GymEntranceScanResponse result,
        Long categoryId,
        String categoryName,
        Double entryPrice) {}
