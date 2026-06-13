package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.model.enums.GymStatus;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Builder
public record GymInfoAdminResponseV2(

        @Schema(description = "Zal ID")
        Long id,

        @Schema(description = "Kateqoriyalar (isMain field-i ilə)")
        List<CategoryWithMainResponse> categories,

        @Schema(description = "Zal adı")
        String name,

        @Schema(description = "Haqqında / Təsvir")
        String description,

        @Schema(description = "Əsas şəkil URL")
        String coverImageUrl,

        @Schema(description = "Otaqlar")
        List<RoomImageDtoV2> rooms,

        @Schema(description = "Telefon")
        String phone,

        @Schema(description = "E-poçt")
        String email,

        @Schema(description = "Şəhər")
        String city,

        @Schema(description = "Ünvan")
        String address,

        @Schema(description = "Enlik")
        Double latitude,

        @Schema(description = "Uzunluq")
        Double longitude,

        @Schema(description = "Status")
        GymStatus status,

        @Schema(description = "Yaradılma tarixi")
        String createdAt,

        @Schema(description = "Dərs növləri")
        List<LessonTypeResponse> lessonTypes

) {}
