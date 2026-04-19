package az.fitnest.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTeacherQueryRequest {
    @NotNull
    private Long gymId;
    @NotNull
    private Long categoryId;
    @NotNull
    private Long lessonTypeId;
}
