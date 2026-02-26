/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymImageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymRoomDto {
    private String room_name;
    private List<GymImageDto> images;
}
