package az.fitnest.catalog.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymPlanItemDto {
    private String plan_id;
    private String packageName;
    private List<GymPlanBenefitDto> benefits;
}
