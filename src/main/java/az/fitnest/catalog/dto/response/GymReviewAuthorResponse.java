package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;

@Builder
public record GymReviewAuthorResponse(
    String user_id,
    String full_name,
    String avatar_url
) {}
