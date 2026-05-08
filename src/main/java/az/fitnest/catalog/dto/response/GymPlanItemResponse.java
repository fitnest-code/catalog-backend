package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;
import lombok.Builder;
import java.util.List;
@Builder
public record GymPlanItemResponse(
    String plan_id,
    String packageName,
    Double dailyPrice,
    List<GymPlanBenefitResponse> benefits
) {}
