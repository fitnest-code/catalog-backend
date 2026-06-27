package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.model.enums.GymStatus;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Builder
public record GymInfoAdminResponseV2(
    @Schema(description = "Zal ID")
    Long id,
    @Schema(description = "Kateqoriyalar")
    List<CategoryResponse> categories,
    @Schema(description = "Əsas Kateqoriyalar")
    List<CategoryResponse> mainCategories,
    @Schema(description = "Alt Kateqoriyalar")
    List<CategoryResponse> subCategories,
    @Schema(description = "Əsas Kateqoriya")
    CategoryResponse category,
    @Schema(description = "Alt Kateqoriya")
    CategoryResponse subCategory,
    @Schema(description = "Kateqoriya-spesifik təsvirlər və nömrələr")
    List<GymDescriptionResponse> descriptions,
    @Schema(description = "Zal adı")
    String name,
    @Schema(description = "Haqqında / Təsvir")
    String description,
    @Schema(description = "Əsas şəkil URL")
    String coverImageUrl,
    @Schema(description = "Digər otaq şəkilləri")
    List<RoomImageDtoV2> rooms,
    @Schema(description = "Telefon nömrəsi")
    String phone,
    @Schema(description = "E-poçt ünvanı")
    String email,
    @Schema(description = "Şəhər")
    String city,
    @Schema(description = "Ünvan")
    String address,
    @Schema(description = "Enlik")
    Double latitude,
    @Schema(description = "Uzunluq")
    Double longitude,
    @Schema(description = "Hündürlük (Altitude)")
    Double altitude,
    @Schema(description = "Status")
    GymStatus status,
    @Schema(description = "Yaradılma tarixi")
    String createdAt,
    @Schema(description = "Alt kateqoriyalar mövcuddur flag")
    Boolean hasSubcategories,
    @Schema(description = "Zalın dərs növləri")
    List<LessonTypeResponse> lessonTypes
) {}
