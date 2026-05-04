package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.dto.response.CategoryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymDetailResponse(
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
    List<GymRoomResponse> rooms,
    List<GymTrainerResponse> trainers,
    List<GymReviewResponse> recent_reviews,
    List<CategoryResponse> categories,
    String coverImageUrl,
    Double rating,
    Integer reviewsCount,
    String qr_code_url,
    GymStatus status,
    List<GymPlanItemResponse> supportedSubscriptions
) {}
