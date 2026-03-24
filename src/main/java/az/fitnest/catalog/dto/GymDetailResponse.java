package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.GymStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymDetailResponse(
    String gym_id,
    String name,
    String description,
    LocationDto address,
    Boolean isSaved,
    String phone,
    String email,
    java.util.Set<GymWorkHourDto> general_work_hours,
    java.util.Set<GymWorkHourDto> work_hours_woman,
    java.util.Set<GymWorkHourDto> work_hours_man,
    List<GymRoomDto> rooms,
    List<GymTrainerDto> trainers,
    List<GymReviewDto> recent_reviews,
    List<CategoryDto> categories,
    String coverImageUrl,
    Double rating,
    Integer reviewsCount,
    String qr_code_url,
    GymStatus status,
    List<GymPlanItemDto> supportedSubscriptions
) {}
