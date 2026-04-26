package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
@Schema(description = "Admin view of a store, including name, address, and phone.")
public record AdminStoreResponse(
    @Schema(description = "Unique identifier of the store", example = "123")
    Long id,

    @Schema(description = "Name of the store", example = "Health Mart")
    String name,

    @Schema(description = "Full address (city + address)", example = "Baku, Neftchilar ave. 5")
    String fullAddress,

    @Schema(description = "Phone number of the store", example = "050-123-45-67")
    String phone,

    @Schema(description = "Status of the store", example = "ACTIVE")
    String status
) {}
