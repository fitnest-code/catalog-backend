package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * @author: nijataghayev
 */

public record GymCategoryRequest(

        @NotNull(message = "Kateqoriya ID boş ola bilməz")
        Long categoryId,

        boolean isMain
) {
}
