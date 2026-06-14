package az.fitnest.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLessonResponse {
    private Long gymId;
    private Long categoryId;
    private Long lessonId;
    private String lessonName;
}
