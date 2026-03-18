package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GymCountResponse {
    private long count;
    private String type;
    private Long subscriptionId;
    private Long categoryId;
}
