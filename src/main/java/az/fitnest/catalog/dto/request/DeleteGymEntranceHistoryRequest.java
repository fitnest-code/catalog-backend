package az.fitnest.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Seçilmiş müştəri giriş tarixçəsi qeydlərini silmək üçün sorğu")
public record DeleteGymEntranceHistoryRequest(

        @Schema(description = "Silinəcək giriş tarixçəsi qeydlərinin ID-ləri", example = "[1, 2, 3]")
        @NotEmpty(message = "Ən azı bir qeyd seçilməlidir")
        List<Long> ids
) {}
