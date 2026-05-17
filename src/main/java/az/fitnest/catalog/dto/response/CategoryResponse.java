package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;

@Builder
public record CategoryResponse(
    Long id,
    String name,
    String photoUrl,
    String iconUrl,
    java.util.List<LessonTypeResponse> lessonTypes
) {}
