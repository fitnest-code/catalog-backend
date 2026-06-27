package az.fitnest.catalog.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record GymTrainerResponseV2(
    String trainer_id,
    String name,
    String surname,
    ProfessionResponse profession,
    String picture,
    String phone,
    String email,
    List<Long> lessonTypeIds,
    List<Long> categoryIds
) {}
