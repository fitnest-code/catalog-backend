package az.fitnest.catalog.dto.v5;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Main or subcategory offering on gym detail v5")
public record GymCategoryOfferingV5(
        Long categoryId,
        String name,
        String role,
        String phone,
        String description,
        String coverImageUrl,
        List<String> imageUrls,
        List<ServiceItem> services,
        List<WorkHourItem> workHours,
        List<PackagePriceItem> packagePrices) {

    public record ServiceItem(Long id, String name, String iconUrl) {}

    public record WorkHourItem(String period, String from, String to) {}

    public record PackagePriceItem(
            Long packageId, String packageName, Double singlePrice, Double monthlyPrice) {}
}
