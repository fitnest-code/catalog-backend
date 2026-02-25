package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymMainPageResponseDto {
    private List<GymMainPageDto> items;
    private long total;
    private int page;
    private int pageSize;
}
