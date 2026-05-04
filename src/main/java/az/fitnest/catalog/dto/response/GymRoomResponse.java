package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;
import java.util.List;
import lombok.Builder;
@Builder
public record GymRoomResponse(
    Long id,
    String room_name,
    List<String> urls
) {}
