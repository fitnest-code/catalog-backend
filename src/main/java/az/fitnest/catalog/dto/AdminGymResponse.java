package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.enums.GymStatus;
import lombok.Builder;

@Builder
public record AdminGymResponse(
    Long id,
    String name,
    String fullAddress, // city + addressText together
    String ownerName,
    GymStatus status
) {}
