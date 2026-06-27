package az.fitnest.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.util.List;
import java.util.Set;
import az.fitnest.catalog.dto.response.GymWorkHourResponse;

@Builder
public record GymCreateCompleteRequestV2(
    // Step 1: Gym Info
    @NotEmpty(message = "Kateqoriya tələb olunur")
    List<CategoryDetail> mainCategoryDetails,

    List<CategoryDetail> subCategoryDetails,

    Boolean hasSubcategories,

    @NotBlank(message = "Ad boş ola bilməz")
    @Size(min = 2, max = 100)
    String name,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(\\+994|0)?\\s?(10|50|51|55|60|70|77|99)(\\s?\\d){7}$", message = "Yanlış mobil nömrə formatı")
    String phone,

    @Size(max = 2000)
    String description,

    @Email(message = "Yanlış email formatı")
    String email,

    List<Long> lessonTypeIds,

    // Step 2: Trainer metadata (photos sent as separate multipart parts)
    List<TrainerCreateData> trainers,

    // Step 3: Working Hours
    Set<GymWorkHourResponse> generalWorkHours,
    Set<GymWorkHourResponse> workHoursWoman,
    Set<GymWorkHourResponse> workHoursMan,
    Set<RestDayRequest> restDays,

    // Step 4: Address
    @NotNull Double latitude,
    @NotNull Double longitude,
    Double altitude,

    // Step 5: Room names and category IDs (files sent as separate multipart parts)
    List<String> roomNames,
    List<Long> roomCategoryIds,

    // Step 6: Subscriptions
    @NotEmpty @Valid List<GymCreateStep6SubscriptionRequestV2> subscriptions,

    // Step 7: Admins
    @NotEmpty @Valid List<GymAdminCreateRequest> admins
) {
    @Builder
    public record TrainerCreateData(
        String name,
        String surname,
        Long professionId,
        String email,
        String phone,
        String lessonTypeIds // comma-separated
    ) {}
}
