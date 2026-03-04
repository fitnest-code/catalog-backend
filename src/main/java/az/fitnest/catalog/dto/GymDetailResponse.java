package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.GymStatus;
import lombok.Builder;
import java.util.List;

@Builder
public record GymDetailResponse(
    String gym_id,
    String name,
    String description,
    LocationDto address,
    Boolean isSaved,
    String phone,
    String email,
    List<GymWorkHourDto> work_hours,
    List<GymRoomDto> rooms,
    List<GymPlanItemDto> membership_plans,
    List<GymTrainerDto> trainers,
    List<GymReviewDto> recent_reviews,
    List<CategoryDto> categories,
    String coverImageUrl,
    Double rating,
    Integer reviewsCount,
    String qr_code_url,
    GymStatus status
) {}
