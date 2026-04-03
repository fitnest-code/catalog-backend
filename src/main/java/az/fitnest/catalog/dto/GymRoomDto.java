package az.fitnest.catalog.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymRoomDto {
    private Long id;
    private String room_name;
    private String url;

}
