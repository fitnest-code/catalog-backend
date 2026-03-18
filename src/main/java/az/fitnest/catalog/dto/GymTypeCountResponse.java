package az.fitnest.catalog.dto;
import lombok.Builder;
@Builder
public record GymTypeCountResponse(String type, long count) {}
