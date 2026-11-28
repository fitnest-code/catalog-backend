package az.fitnest.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.util.List;
import java.util.Set;
import az.fitnest.catalog.dto.response.GymWorkHourResponse;

/** V3 of the one-shot create: V2 contract plus EN/RU values for translatable free text. */
@Builder
public record GymCreateCompleteRequestV3(
    // Step 1: Gym info
    @NotEmpty(message = "Kateqoriya tələb olunur")
    @Valid
    List<CategoryDetailV3> mainCategoryDetails,

    @Valid
    List<CategoryDetailV3> subCategoryDetails,

    Boolean hasSubcategories,

    @NotBlank(message = "Ad boş ola bilməz")
    @Size(min = 2, max = 100)
    String name,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(\\+994|0)?\\s?(10|50|51|55|60|70|77|99)(\\s?\\d){7}$", message = "Yanlış mobil nömrə formatı")
    String phone,

    @Size(max = 2000)
    String description,

    @Size(max = 2000)
    String descriptionEn,

    @Size(max = 2000)
    String descriptionRu,

    @Email(message = "Yanlış email formatı")
    String email,

    List<Long> lessonTypeIds,

    // Step 2: Trainer metadata (photos are separate multipart parts)
    @Valid
    List<TrainerCreateData> trainers,

    // Step 3: Working hours
    Set<GymWorkHourResponse> generalWorkHours,
    Set<GymWorkHourResponse> workHoursWoman,
    Set<GymWorkHourResponse> workHoursMan,
    Set<RestDayRequest> restDays,

    // Step 4: Address
    @NotNull Double latitude,
    @NotNull Double longitude,
    Double altitude,
    String city,
    String rayon,
    String addressText,

    // Step 5: Room metadata
    List<String> roomNames,
    List<Long> roomCategoryIds,

    // Step 6: Subscriptions / services
    @NotEmpty @Valid List<GymCreateStep6SubscriptionRequestV3> subscriptions,

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
        String lessonTypeIds
    ) {}
}
