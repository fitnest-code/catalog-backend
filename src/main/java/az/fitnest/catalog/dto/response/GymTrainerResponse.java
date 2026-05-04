package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import az.fitnest.catalog.dto.response.ProfessionResponse;
import lombok.Builder;

@Builder
public record GymTrainerResponse(
    String trainer_id,
    String name,
    String surname,
    ProfessionResponse profession,
    String picture,
    String phone,
    String email
) {}
