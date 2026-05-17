package az.fitnest.catalog.dto.response;

import lombok.Builder;

@Builder
public record GymAdminResponse(
    Long id,
    Long userId,
    String name,
    String surname,
    String phone,
    String email,
    String role
) {}
