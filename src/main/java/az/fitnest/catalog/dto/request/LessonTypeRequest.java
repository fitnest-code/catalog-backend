package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LessonTypeRequest {
    @NotBlank(message = "Lesson type name is required")
    private String name;
}
