package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GymLessonTypeRequest {
    @NotBlank(message = "Lesson type name is required")
    private String name;
}
