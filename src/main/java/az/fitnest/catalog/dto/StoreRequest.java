package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import az.fitnest.catalog.dto.StoreWorkHourDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Schema(description="Request to create or update a store")
public class StoreRequest {
    @NotBlank
    @Schema(description="Store name", example="Fit Market")
    private String name;
    
    @Schema(description="Store description")
    private String description;
    
    @Schema(description="Store address")
    private AddressDto address;
    
    @Schema(description="Store phone")
    private String phone;
    
    @Schema(description="Store category", example="SUPPLEMENTS")
    private String category;
    
    @Schema(description="Store status", example="ACTIVE")
    private String status;
    
    @Schema(description="Store work hours")
    private List<StoreWorkHourDto> workingHours;
    
    @Schema(description="Store social media links")
    private StoreSocialDto social;
}
