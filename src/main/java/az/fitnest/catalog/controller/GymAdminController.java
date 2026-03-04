package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymSubscriptionsUpdateRequest;
import az.fitnest.catalog.service.impl.GymReadService;
import az.fitnest.catalog.service.impl.GymWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Gym Admin", description = "İdman zallarını idarə etmək üçün administrativ ucluqlar. Bu ucluqlar yalnız ADMIN və SUPER_ADMIN rollarına malik istifadəçilər tərəfindən istifadə edilə bilər.")
@SecurityRequirement(name = "bearerAuth")
public class GymAdminController {

    private final GymWriteService gymWriteService;
    private final GymReadService gymReadService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Operation(summary = "Yeni idman zalı yaradın", description = "Sistemə yeni idman zalı əlavə edir (üz qabığı şəkli ilə birlikdə). ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createGym(
            @RequestPart("cover") MultipartFile cover,
            @Parameter(description = "Gym details in JSON format") 
            @RequestPart("request") String requestStr) {
        
        GymRequest request;
        try {
            request = objectMapper.readValue(requestStr, GymRequest.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new az.fitnest.catalog.exception.BadRequestException("INVALID_JSON", "Məlumat formatı düzgün deyil");
        }

        // Validate the deserialized object manually
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();
        java.util.Set<jakarta.validation.ConstraintViolation<GymRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }

        gymWriteService.createGym(request, cover);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "İdman zalını yeniləyin", description = "Mövcud idman zalının məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateGym(@PathVariable Long id, @Valid @RequestBody GymRequest request) {
        gymWriteService.updateGym(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalı abunəliklərini yeniləyin", description = "İdman zalı üçün mövcud abunəlik planlarını və üstünlüklərini yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/subscriptions")
    public ResponseEntity<Void> updateGymSubscriptions(@PathVariable Long id, @Valid @RequestBody GymSubscriptionsUpdateRequest request) {
        gymWriteService.updateGymSubscriptions(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalını silin", description = "İdman zalını sistemdən silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGym(@PathVariable Long id) {
        gymWriteService.deleteGym(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalına otaq şəkli əlavə edin", description = "İdman zalı üçün otaq şəkli yükləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addRoomImage(
            @PathVariable Long id,
            @RequestParam("roomName") String roomName,
            @RequestParam("file") MultipartFile file) {
        gymWriteService.addRoomImage(id, roomName, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalı üz qabığı şəklini yeniləyin", description = "İdman zalı üçün əsas profil şəklini yükləyir və ya yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCoverImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        gymWriteService.updateCoverImage(id, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün idman zallarını silin (Kritik)", description = "Sistemdəki BÜTÜN idman zallarını silir. Bu əməliyyat üçün SUPER_ADMIN rolu tələb olunur və bu hərəkət geri qaytarıla bilməz.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllGyms() {
        gymWriteService.deleteAllGyms();
        return ResponseEntity.noContent().build();
    }
}
