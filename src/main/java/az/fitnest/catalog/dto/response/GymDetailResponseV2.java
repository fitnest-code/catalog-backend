package az.fitnest.catalog.dto.response;

import az.fitnest.catalog.dto.request.RestDayRequest;
import az.fitnest.catalog.model.enums.GymStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymDetailResponseV2(
    String gym_id,
    String name,
    String description,
    LocationResponse address,
    Boolean isSaved,
    String phone,
    String email,
    List<GymWorkHourResponse> general_work_hours,
    List<GymWorkHourResponse> work_hours_woman,
    List<GymWorkHourResponse> work_hours_man,
    List<GymRoomResponseV2> rooms,
    List<GymTrainerResponse> trainers,
    List<GymReviewResponse> recent_reviews,
    List<CategoryResponse> categories,
    String coverImageUrl,
    Double rating,
    Integer reviewsCount,
    String qr_code_url,
    GymStatus status,
    List<GymPlanItemResponseV2> supportedSubscriptions,
    List<RestDayRequest> rest_days
) {}
