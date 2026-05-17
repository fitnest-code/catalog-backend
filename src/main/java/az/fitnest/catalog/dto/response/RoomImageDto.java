package az.fitnest.catalog.dto.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record RoomImageDto(
    @Schema(description = "Otaq ID")
    Long id,
    @Schema(description = "Otaq adı")
    String name,
    @Schema(description = "Şəkil URL")
    String imageUrl
) {}
