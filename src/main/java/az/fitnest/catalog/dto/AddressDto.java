package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@JsonDeserialize(using = AddressDtoDeserializer.class)
public record AddressDto(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Resolved address text", example = "Baku, 28 May str. 12", accessMode = Schema.AccessMode.READ_ONLY)
    String addressText,

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "City", example = "Baku", accessMode = Schema.AccessMode.READ_ONLY)
    String city,

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Latitude")
    @NotNull(message = "Latitude cannot be null")
    Double latitude,

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Longitude")
    @NotNull(message = "Longitude cannot be null")
    Double longitude
) {}
