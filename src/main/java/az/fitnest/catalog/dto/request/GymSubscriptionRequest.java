package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import java.util.List;

@Builder
public record GymSubscriptionRequest(
    Long planId,
    List<GymSubscriptionBenefitRequest> benefits
) {}
