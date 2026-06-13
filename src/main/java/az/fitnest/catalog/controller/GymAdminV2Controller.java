package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.request.GymCreateCompleteRequestV2;
import az.fitnest.catalog.dto.request.GymCreateStep1RequestV2;
import az.fitnest.catalog.dto.request.GymCreateStep6RequestV2;
import az.fitnest.catalog.dto.request.GymInfoUpdateRequestV2;
import az.fitnest.catalog.dto.response.GymCreateStep1Response;
import az.fitnest.catalog.dto.response.GymInfoAdminResponseV2;
import az.fitnest.catalog.dto.response.GymSubscriptionsAdminResponseV2;
import az.fitnest.catalog.service.GymReadService;
import az.fitnest.catalog.service.GymWriteService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/gyms")
@RequiredArgsConstructor
@Tag(
        name = "Gym Admin V2",
        description = "İdman zallarını idarə etmək üçün V2 administrativ "
                + "ucluqlar. Çoxlu kateqoriya və isMain dəstəyi ilə."
)
@SecurityRequirement(name = "bearerAuth")
public class GymAdminV2Controller {

    private final GymWriteService gymWriteService;
    private final GymReadService gymReadService;

    @Operation(
            summary = "Tam idman zalı yaradın (V2)",
            description = "Bütün 7 addımı birləşdirərək idman zalını V2 ilə "
                    + "bir dəfəyə yaradır. Hər kateqoriya üçün isMain dəstəklənir."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/create-complete",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GymCreateStep1Response> createGymComplete(
            @RequestPart("data") @Valid GymCreateCompleteRequestV2 request,
            @RequestPart(value = "coverPhoto", required = false)
            MultipartFile coverPhoto,
            @RequestPart(value = "trainerPhotos", required = false)
            List<MultipartFile> trainerPhotos,
            @RequestPart(value = "roomPhotos", required = false)
            List<MultipartFile> roomPhotos,
            @RequestPart(value = "serviceIcons", required = false)
            List<MultipartFile> serviceIcons) {
        Long gymId = gymWriteService.createGymCompleteV2(
                request, coverPhoto, trainerPhotos, roomPhotos, serviceIcons);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GymCreateStep1Response(gymId));
    }

    @Operation(
            summary = "Step 1: Yeni idman zalı yaradın (V2, DRAFT)",
            description = "Çoxlu kateqoriya + isMain dəstəyi ilə zal yaradır."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/step1")
    public ResponseEntity<GymCreateStep1Response> createGymStep1(
            @Valid @RequestBody GymCreateStep1RequestV2 request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gymWriteService.createGymStep1V2(request));
    }

    @Operation(
            summary = "Validate Step 1 (V2)",
            description = "Kateqoriya ID-lərini və isMain field-lərini yoxlayır."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/validate/step1")
    public ResponseEntity<Void> validateStep1(
            @Valid @RequestBody GymCreateStep1RequestV2 request) {
        gymWriteService.validateStep1V2(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Step 5: İdman zalına şəkillər əlavə edin (V2)",
            description = "Örtük şəkli və otaq şəkillərini yükləyir. "
                    + "Hər otaq üçün kateqoriya ID-si göndərilə bilər."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/step5",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createGymStep5(
            @PathVariable Long id,
            @RequestParam("coverPhoto") MultipartFile coverPhoto,
            @RequestParam(value = "roomNames", required = false)
            List<String> roomNames,
            @RequestParam(value = "roomCategoryIds", required = false)
            List<Long> roomCategoryIds,
            @RequestParam(value = "roomPhotos", required = false)
            List<MultipartFile> roomPhotos) {
        gymWriteService.createGymStep5V2(
                id, coverPhoto, roomNames, roomCategoryIds, roomPhotos);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Validate Step 5 (V2)",
            description = "Şəkil formatlarını və otaq kateqoriya ID-lərini yoxlayır."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/validate/step5",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> validateStep5(
            @RequestParam(value = "coverPhoto", required = false)
            MultipartFile coverPhoto,
            @RequestParam(value = "roomNames", required = false)
            List<String> roomNames,
            @RequestParam(value = "roomCategoryIds", required = false)
            List<Long> roomCategoryIds,
            @RequestParam(value = "roomPhotos", required = false)
            List<MultipartFile> roomPhotos) {
        gymWriteService.validateStep5V2(
                coverPhoto, roomNames, roomCategoryIds, roomPhotos);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Step 6: Abunəlik və xidmətləri aktivləşdirin (V2)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/step6",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createGymStep6(
            @PathVariable Long id,
            @RequestPart("data") @Valid GymCreateStep6RequestV2 request,
            @RequestPart(value = "serviceIcons", required = false)
            List<MultipartFile> serviceIcons) {
        gymWriteService.createGymStep6V2(id, request, serviceIcons);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Validate Step 6 (V2)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/validate/step6",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> validateStep6(
            @RequestPart("data") @Valid GymCreateStep6RequestV2 request,
            @RequestPart(value = "serviceIcons", required = false)
            List<MultipartFile> serviceIcons) {
        gymWriteService.validateStep6V2(request, serviceIcons);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Zal abunəliklərini alın (V2)",
            description = "Hər abunəlik üçün kateqoriya ID-si ilə birlikdə qaytarır."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/subscriptions")
    public ResponseEntity<GymSubscriptionsAdminResponseV2> getGymSubscriptions(
            @PathVariable("id") Long gymId) {
        return ResponseEntity.ok(gymReadService.getGymSubscriptionsV2(gymId));
    }

    @Operation(summary = "İdman zalı abunəliklərini yeniləyin (V2)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/subscriptions")
    public ResponseEntity<Void> updateGymSubscriptions(
            @PathVariable Long id,
            @Valid @RequestBody GymCreateStep6RequestV2 request) {
        gymWriteService.updateGymSubscriptionsV2(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Zal məlumatlarını alın (V2)",
            description = "Hər kateqoriya üçün isMain field-i ilə birlikdə qaytarır."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/{id}/details")
    public ResponseEntity<GymInfoAdminResponseV2> getGymDetails(
            @PathVariable("id") Long gymId) {
        return ResponseEntity.ok(gymReadService.getGymDetailsAdminV2(gymId));
    }

    @Operation(
            summary = "Zal məlumatlarını yeniləyin (V2)",
            description = "Kateqoriyalar isMain field-i ilə göndərilməlidir."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/details")
    public ResponseEntity<Void> updateGymDetails(
            @PathVariable("id") Long gymId,
            @Valid @RequestBody GymInfoUpdateRequestV2 request) {
        gymWriteService.updateGymInfoV2(gymId, request);
        return ResponseEntity.ok().build();
    }
}