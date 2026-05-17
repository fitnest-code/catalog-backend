package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;
import lombok.Builder;
@Builder
public record GymSocialLinkResponse(
    String name,
    String url
) {}
