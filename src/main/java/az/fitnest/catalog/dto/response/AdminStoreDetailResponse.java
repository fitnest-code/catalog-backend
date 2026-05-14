package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: nijataghayev
 */

@Builder
@Schema(description = "Admin paneli üçün mağaza detalları")
public record AdminStoreDetailResponse(

        @Schema(description = "Mağazanın unikal identifikatoru", example = "1")
        Long id,

        @Schema(description = "Mağazanın adı", example = "FitLife Market")
        String name,

        @Schema(description = "Mağazanın statusu", example = "ACTIVE")
        String status,

        @Schema(description = "Mağazanın kateqoriyası", example = "GYM")
        String category,

        @Schema(description = "Üz qabığı şəklinin URL-i")
        String coverImageUrl,



        @Schema(description = "Məkan məlumatları")
        AddressDto address,

        @Schema(description = "Əlaqə nömrəsi", example = "+994501234567")
        String phone,

        @Schema(description = "Email ünvanı", example = "market@example.com")
        String email,

        @Schema(description = "Sosial media linki")
        SocialLinkDto socialLink,

        @Schema(description = "İş saatları")
        WorkHoursResponseDto workHours,

        @Schema(description = "Paket endirimləri")
        List<DiscountResponseDto> discounts,

        @Schema(description = "Mağaza şəkilləri")
        List<StoreImageDto> images,

        @Schema(description = "Populyarlıq balı", example = "4.5")
        Double popularScore,

        @Schema(description = "Yaradılma tarixi")
        LocalDateTime createdDate,

        @Schema(description = "Son yenilənmə tarixi")
        LocalDateTime lastModifiedDate,

        @Schema(description = "Yaradan istifadəçi")
        String createdBy,

        @Schema(description = "Son dəyişdirən istifadəçi")
        String lastModifiedBy
) {

    @Builder
    @Schema(description = "Ünvan məlumatları")
    public record AddressDto(
            @Schema(description = "Ünvan mətni", example = "Nəriman Nərimanov rayonu")
            String addressText,

            @Schema(description = "Şəhər", example = "Bakı")
            String city,

            @Schema(description = "Coğrafi enlik", example = "40.4093")
            Double latitude,

            @Schema(description = "Coğrafi uzunluq", example = "49.8671")
            Double longitude
    ) {
    }

    @Builder
    @Schema(description = "Sosial media linki")
    public record SocialLinkDto(
            @Schema(description = "Platforma adı", example = "Instagram")
            String name,

            @Schema(description = "Link", example = "https://instagram.com/market")
            String url
    ) {
    }

    @Builder
    @Schema(description = "İş saatları")
    public record WorkHoursResponseDto(
            @Schema(description = "Açılış saatı", example = "09:00")
            String from,

            @Schema(description = "Bağlanış saatı", example = "22:00")
            String to
    ) {
    }

    @Builder
    @Schema(description = "Paket endirimi")
    public record DiscountResponseDto(
            @Schema(description = "Paketin ID-si", example = "1")
            Long packageId,

            @Schema(description = "Endirim faizi", example = "15")
            Integer discountPercent
    ) {
    }

    @Builder
    @Schema(description = "Mağaza şəkli")
    public record StoreImageDto(
            @Schema(description = "Şəklin tipi", example = "GALLERY")
            String type,

            @Schema(description = "Şəklin başlığı", example = "Zalın görünüşü")
            String title,

            @Schema(description = "Şəklin URL-i")
            String url
    ) {
    }
}
