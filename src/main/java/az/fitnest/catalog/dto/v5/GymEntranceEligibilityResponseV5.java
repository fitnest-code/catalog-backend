package az.fitnest.catalog.dto.v5;

import java.util.List;

public record GymEntranceEligibilityResponseV5(
        boolean eligible,
        String reason,
        boolean categorySelectionRequired,
        List<CategoryChip> categories) {

    public record CategoryChip(Long categoryId, String name, String role, String coverImageUrl) {}
}
