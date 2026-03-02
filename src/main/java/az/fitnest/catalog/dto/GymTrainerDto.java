package az.fitnest.catalog.dto;

import lombok.Builder;

@Builder
public record GymTrainerDto(
    String trainer_id,
    String name,
    String surname,
    ProfessionDto profession,
    String picture,
    String phone,
    String email
) {}
