package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import az.fitnest.catalog.model.enums.GymStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;

@Builder
public record GymRequest(
    @NotBlank(message = "Ad boş ola bilməz")
    @jakarta.validation.constraints.Size(min = 2, max = 100, message = "Ad 2-100 simvol arası olmalıdır")
    @Schema(description = "İdman zalının adı", example = "FitLife Premium")
    String name,

    @jakarta.validation.constraints.Size(max = 2000, message = "Təsvir çox uzundur")
    @Schema(description = "İdman zalının təsviri", example = "Bakının mərkəzində müasir idman zalı")
    String description,

    @NotNull(message = "Ünvan boş ola bilməz")
    @Valid
    @Schema(description = "İdman zalının ünvanı")
    AddressResponse address,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(\\+994|0|994)?\\s?\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}$", message = "Yanlış mobil nömrə formatı.")
    @Schema(description = "Əlaqə telefonu", example = "0501234567")
    String phone,

    @Email(message = "Yanlış email formatı")
    @Schema(description = "Əlaqə emaili", example = "info@fitlife.az")
    String email,

    @NotEmpty(message = "Kateqoriyalar boş ola bilməz")
    @Schema(description = "Kateqoriya ID-ləri", example = "[1, 2]")
    Set<Long> categoryIds,

    @NotNull(message = "Status boş ola bilməz")
    @Schema(description = "İdman zalının statusu", example = "ACTIVE")
    GymStatus status,

    @Schema(description = "Ümumi iş saatları")
    java.util.Set<GymWorkHourResponse> generalWorkHours,
    @Schema(description = "Qadınlar zalı iş saatları")
    java.util.Set<GymWorkHourResponse> workHoursWoman,
    @Schema(description = "Kişilər zalı iş saatları")
    java.util.Set<GymWorkHourResponse> workHoursMan,
    @Schema(description = "İstirahət günləri")
    java.util.Set<RestDayRequest> restDays
) {}
