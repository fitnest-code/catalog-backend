package az.fitnest.catalog.dto.v5;

import az.fitnest.catalog.dto.response.GymDetailResponseV4;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Gym detail v5: v4 payload + full category offerings (main + sub)")
public record GymDetailResponseV5(
        GymDetailResponseV4 detail,
        boolean hasSubcategories,
        boolean categorySelectionRequired,
        List<GymCategoryOfferingV5> categories) {}
