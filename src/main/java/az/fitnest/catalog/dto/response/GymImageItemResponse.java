package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;

@Builder
public record GymImageItemResponse(
    String image_id,
    String type,
    String title,
    String url
) {}
