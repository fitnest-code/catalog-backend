package az.fitnest.catalog.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record GymRoomResponseV2(
    Long id,
    String room_name,
    Long categoryId,
    List<String> urls
) {}
