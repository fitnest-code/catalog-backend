package az.fitnest.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author: nijataghayev
 */

@Builder
public record CategoryWithMainResponse(

        @Schema(description = "Kateqoriya ID")
        Long id,

        @Schema(description = "Kateqoriya adı (lokallaşdırılmış)")
        String name,

        @Schema(description = "Foto URL")
        String photoUrl,

        @Schema(description = "İkon URL")
        String iconUrl,

        @Schema(description = "Əsas kateqoriyadır? true=main, false=sub")
        boolean isMain

) {}
