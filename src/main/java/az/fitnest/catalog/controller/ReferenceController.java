package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.ApiResponse;
import az.fitnest.catalog.dto.admin.CityDto;
import az.fitnest.catalog.dto.admin.DistrictDto;
import az.fitnest.catalog.service.impl.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "Kataloqda istifadə olunan referans məlumatları: şəhərlər və rayonlar")
@Validated
public class ReferenceController {

    private final LocationService locationService;

    @GetMapping("/cities")
    @Operation(summary = "Şəhər siyahısını qaytarır")
    public ResponseEntity<ApiResponse<List<CityDto>>> getCities() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getCities()));
    }

    @GetMapping("/districts")
    @Operation(summary = "Seçilmiş şəhərə uyğun rayon siyahısını qaytarır")
    public ResponseEntity<ApiResponse<List<DistrictDto>>> getDistricts(
            @RequestParam @NotNull(message = "cityId boş ola bilməz") Long cityId
    ) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getDistricts(cityId)));
    }
}