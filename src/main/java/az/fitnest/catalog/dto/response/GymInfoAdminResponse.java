package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.dto.request.RestDayRequest;
import az.fitnest.catalog.model.enums.GymStatus;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

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
    @Schema(description = "Work hours")
    Set<GymWorkHourResponse> generalWorkHours,
    @Schema(description = "Work hours (Woman)")
    Set<GymWorkHourResponse> workHoursWoman,
    @Schema(description = "Work hours (Man)")
    Set<GymWorkHourResponse> workHoursMan,
    @Schema(description = "Rest days")
    Set<RestDayRequest> restDays,
    @Schema(description = "Status")
    GymStatus status,
    @Schema(description = "Yaradılma tarixi")
    String createdAt
) {}
