package az.fitnest.catalog.dto.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Builder
public record GymInfoAdminResponse(
    @Schema(description = "Zal ID")
    Long id,
    @Schema(description = "Kateqoriya ID")
    Long categoryId,
    @Schema(description = "Kateqoriya adı")
    String categoryName,
    @Schema(description = "Zal adı")
    String name,
    @Schema(description = "Haqqında / Təsvir")
    String description,
    @Schema(description = "Əsas şəkil URL")
    String coverImageUrl,
    @Schema(description = "Digər otaq şəkilləri")
    List<RoomImageDto> rooms,
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
    @Schema(description = "Yaradılma tarixi")
    String createdAt
) {}
