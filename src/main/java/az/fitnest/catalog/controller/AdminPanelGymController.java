package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.ApiResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.model.enums.GymFilterStatus;
import az.fitnest.catalog.model.enums.RatingStatus;
import az.fitnest.catalog.service.impl.AdminPanelGymReadService;
import az.fitnest.catalog.service.impl.AdminPanelGymWriteService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Admin Panel Gyms", description = "Admin panelində idman zallarını idarə etmək və kəşf etmək üçün ucluqlar, o cümlədən paketlər, məşqçilər və istifadəçi rəyləri")
@SecurityRequirement(name = "bearerAuth")
public class AdminPanelGymController {

    private final AdminPanelGymReadService gymAdminReadService;
    private final AdminPanelGymWriteService gymAdminWriteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zalların siyahısı", description = "Filtrlər və sıralama əsasında admin paneli üçün zalları paginated şəkildə qaytarır.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Zallar uğurla qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bu əməliyyat üçün icazə yoxdur")
    })
    public ResponseEntity<PaginatedResponse<AdminPanelGymListDto>> getGymsForAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) GymFilterStatus status,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long districtId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") SortDirection sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safeSize = Math.min(size, 100);
        return ResponseEntity.ok(gymAdminReadService.getGymsForAdmin(
                search, status, cityId, districtId,
                sortBy, sortOrder, page, safeSize
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Yeni zal yarat", description = "Admin panelindən yeni idman zalı yaradır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Zal uğurla yaradıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Daxil edilən məlumat yanlışdır"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bu əməliyyat üçün icazə yoxdur")
    })
    public ResponseEntity<ApiResponse<AdminPanelGymResponse>> createGymForAdmin(
            @RequestBody @Valid AdminPanelCreateGymRequest request
    ) {
        AdminPanelGymResponse response = gymAdminWriteService.createGymForAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{gymId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal detalı", description = "Verilən zalın admin panel üçün detallı məlumatlarını qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Zal detalları qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bu əməliyyat üçün icazə yoxdur")
    })
    public ResponseEntity<ApiResponse<AdminPanelGymDetailDto>> getGymForAdmin(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getGymForAdmin(gymId)));
    }

    @PatchMapping("/{gymId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateGymStatus(
            @PathVariable Long gymId,
            @RequestBody @Valid AdminPanelUpdateGymStatusRequest request
    ) {
        gymAdminWriteService.updateGymStatus(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{gymId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal sil", description = "Verilən zalı sistemdən silir.")
    public ResponseEntity<Void> deleteGym(@PathVariable Long gymId) {
        gymAdminWriteService.deleteGym(gymId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/general-info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Ümumi məlumatları yenilə", description = "Zalın ad, təsvir, status, əlaqə və ünvan məlumatlarını yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateGeneralInfo(
            @PathVariable Long gymId,
            @RequestBody @Valid GeneralInfoRequest request
    ) {
        gymAdminWriteService.updateGeneralInfo(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{gymId}/cover-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Cover şəkli yüklə", description = "Zal üçün cover image yükləyir və saxlayır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cover şəkli uğurla yükləndi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Fayl yanlışdır"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bu əməliyyat üçün icazə yoxdur")
    })
    public ResponseEntity<ApiResponse<CoverImageResponse>> uploadCoverImage(
            @PathVariable Long gymId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminWriteService.uploadCoverImage(gymId, file)));
    }

    @DeleteMapping("/{gymId}/cover-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Cover şəkli sil", description = "Zalın cover şəklini silir.")
    public ResponseEntity<Void> deleteCoverImage(@PathVariable Long gymId) {
        gymAdminWriteService.deleteCoverImage(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gymId}/gallery-images")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Qalereya şəkli əlavə et", description = "Zala yeni qalereya şəkli əlavə edir.")
    public ResponseEntity<ApiResponse<Void>> addGalleryImage(
            @PathVariable Long gymId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer sortOrder
    ) {
        gymAdminWriteService.addGalleryImage(gymId, file, sortOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @GetMapping("/{gymId}/gallery-images")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Qalereya şəkilləri", description = "Verilən zalın bütün qalereya şəkillərini qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Qalereya şəkilləri qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<ApiResponse<List<AdminPanelGymImageDto>>> getGalleryImages(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getGalleryImages(gymId)));
    }

    @DeleteMapping("/{gymId}/gallery-images/{imageId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Qalereya şəklini sil", description = "Seçilmiş qalereya şəklini silir.")
    public ResponseEntity<Void> deleteGalleryImage(
            @PathVariable Long gymId,
            @PathVariable Long imageId
    ) {
        gymAdminWriteService.deleteGalleryImage(gymId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/gallery-images/order")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Şəkil sırasını yenilə", description = "Qalereya şəkillərinin sırasını yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateImageOrder(
            @PathVariable Long gymId,
            @RequestBody @Valid UpdateImageOrderRequest request
    ) {
        gymAdminWriteService.updateImageOrder(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{gymId}/working-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - İş saatları", description = "Zalın iş saatlarını qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "İş saatları qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<ApiResponse<List<WorkingHourDto>>> getWorkingHours(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getWorkingHours(gymId)));
    }

    @PostMapping("/{gymId}/working-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - İş saatı əlavə et", description = "Zala yeni iş saatı əlavə edir.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "İş saatı yaradıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Daxil edilən məlumat yanlışdır")
    })
    public ResponseEntity<ApiResponse<WorkingHourDto>> addWorkingHour(
            @PathVariable Long gymId,
            @RequestBody @Valid WorkingHourRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(gymAdminWriteService.addWorkingHour(gymId, request)));
    }

    @PutMapping("/{gymId}/working-hours/{workingHourId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - İş saatını yenilə", description = "Mövcud iş saatını yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateWorkingHour(
            @PathVariable Long gymId,
            @PathVariable Long workingHourId,
            @RequestBody @Valid WorkingHourRequest request
    ) {
        gymAdminWriteService.updateWorkingHour(gymId, workingHourId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{gymId}/working-hours/{workingHourId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - İş saatını sil", description = "Mövcud iş saatını silir.")
    public ResponseEntity<Void> deleteWorkingHour(
            @PathVariable Long gymId,
            @PathVariable Long workingHourId
    ) {
        gymAdminWriteService.deleteWorkingHour(gymId, workingHourId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gymId}/trainers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Məşqçilərin siyahısı", description = "Seçilmiş zal üçün məşqçiləri paginated qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Məşqçilər qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<PaginatedResponse<TrainerListDto>> getTrainers(
            @PathVariable Long gymId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(gymAdminReadService.getTrainers(gymId, search, page, Math.min(size, 100)));
    }

    @GetMapping("/{gymId}/trainers/{trainerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Məşqçi detalı", description = "Seçilmiş məşqçi haqqında detal qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Məşqçi detalları qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Məşqçi tapılmadı")
    })
    public ResponseEntity<ApiResponse<TrainerDetailDto>> getTrainer(
            @PathVariable Long gymId,
            @PathVariable Long trainerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getTrainer(gymId, trainerId)));
    }

    @PostMapping(value = "/{gymId}/trainers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Məşqçi əlavə et", description = "Zala yeni məşqçi əlavə edir, lazım olsa şəkil də yükləyir.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Məşqçi yaradıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Daxil edilən məlumat yanlışdır")
    })
    public ResponseEntity<ApiResponse<TrainerDetailDto>> addTrainer(
            @PathVariable Long gymId,
            @RequestPart("data") @Valid AdminPanelTrainerRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(gymAdminWriteService.addTrainer(gymId, request, file)));
    }

    @PutMapping(value = "/{gymId}/trainers/{trainerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Məşqçi yenilə", description = "Mövcud məşqçi məlumatlarını yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateTrainer(
            @PathVariable Long gymId,
            @PathVariable Long trainerId,
            @RequestPart("data") @Valid AdminPanelTrainerRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        gymAdminWriteService.updateTrainer(gymId, trainerId, request, file);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{gymId}/trainers/{trainerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Məşqçini sil", description = "Seçilmiş məşqçini sistemdən silir.")
    public ResponseEntity<Void> deleteTrainer(
            @PathVariable Long gymId,
            @PathVariable Long trainerId
    ) {
        gymAdminWriteService.deleteTrainer(gymId, trainerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscription-types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Subscription tipləri", description = "Sistemdə mövcud subscription tiplərini qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription tipləri qaytarıldı")
    })
    public ResponseEntity<ApiResponse<List<SubscriptionTypeDto>>> getSubscriptionTypes() {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getSubscriptionTypes()));
    }

    @GetMapping("/{gymId}/subscriptions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal subscription-ları", description = "Seçilmiş zalın subscription-larını qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription-lar qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<ApiResponse<List<GymSubscriptionDto>>> getGymSubscriptions(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getGymSubscriptions(gymId)));
    }

    @PutMapping("/{gymId}/subscriptions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal subscription-larını yenilə", description = "Zal üçün subscription seçimlərini yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateGymSubscriptions(
            @PathVariable Long gymId,
            @RequestBody @Valid UpdateGymSubscriptionRequest request
    ) {
        gymAdminWriteService.updateGymSubscriptions(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/service-types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Service tipləri", description = "Sistemdə mövcud olan service tiplərini qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service tipləri qaytarıldı")
    })
    public ResponseEntity<ApiResponse<List<ServiceTypeDto>>> getServiceTypes() {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getServiceTypes()));
    }

    @GetMapping("/{gymId}/services")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal servisləri", description = "Seçilmiş zalın servislərini qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Servislər qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<ApiResponse<List<GymServiceItemDto>>> getGymServices(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getGymServices(gymId)));
    }

    @PostMapping("/service-types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Service tipi yarat", description = "Yeni service tipi yaradır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Service tipi yaradıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Daxil edilən məlumat yanlışdır")
    })
    public ResponseEntity<ApiResponse<ServiceTypeDto>> createServiceType(
            @RequestBody @Valid CreateServiceTypeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(gymAdminWriteService.createServiceType(request)));
    }

    @PutMapping("/{gymId}/services")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal servislərini yenilə", description = "Zalın servislərini yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateGymServices(
            @PathVariable Long gymId,
            @RequestBody @Valid UpdateGymServiceRequest request
    ) {
        gymAdminWriteService.updateGymServices(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{gymId}/admins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal adminləri", description = "Seçilmiş zala bağlı adminləri qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Adminlər qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<ApiResponse<List<GymAdminListDto>>> getAdmins(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getAdmins(gymId)));
    }

    @PostMapping("/{gymId}/admins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal admini yarat", description = "Seçilmiş zala yeni admin əlavə edir.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Admin yaradıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Daxil edilən məlumat yanlışdır")
    })
    public ResponseEntity<ApiResponse<GymAdminListDto>> createAdmin(
            @PathVariable Long gymId,
            @RequestBody @Valid CreateGymAdminRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(gymAdminWriteService.createAdmin(gymId, request)));
    }

    @PutMapping("/{gymId}/admins/{adminId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal adminini yenilə", description = "Mövcud zal admin məlumatlarını yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> updateAdmin(
            @PathVariable Long gymId,
            @PathVariable Long adminId,
            @RequestBody @Valid UpdateGymAdminRequest request
    ) {
        gymAdminWriteService.updateAdmin(gymId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{gymId}/admins/{adminId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Zal adminini sil", description = "Seçilmiş zal adminini silir.")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long gymId,
            @PathVariable Long adminId
    ) {
        gymAdminWriteService.deleteAdmin(gymId, adminId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gymId}/admins/{adminId}/reset-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Admin şifrəsini yenilə", description = "Seçilmiş zal admininin şifrəsini yeniləyir.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long gymId,
            @PathVariable Long adminId,
            @RequestBody @Valid ResetPasswordRequest request
    ) {
        gymAdminWriteService.resetPassword(gymId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{gymId}/ratings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Reytinq", description = "Zal üçün reytinqləri paginated şəkildə qaytarır.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reytinqlər qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Zal tapılmadı")
    })
    public ResponseEntity<PaginatedResponse<RatingListDto>> getRatings(
            @PathVariable Long gymId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RatingStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") SortDirection sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(gymAdminReadService.getRatings(
                gymId, search, status, sortBy, sortOrder, page, Math.min(size, 100)
        ));
    }

    @GetMapping("/{gymId}/ratings/{ratingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Reytinq detalı", description = "Seçilmiş reyting və rəyi detallı göstərir.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reytinq detalı qaytarıldı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rəy tapılmadı")
    })
    public ResponseEntity<ApiResponse<RatingDetailDto>> getRating(
            @PathVariable Long gymId,
            @PathVariable Long ratingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getRating(gymId, ratingId)));
    }

    @PatchMapping("/{gymId}/ratings/{ratingId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Rəyi təsdiqlə", description = "Seçilmiş rəyi təsdiqləyir.")
    public ResponseEntity<ApiResponse<Void>> approveRating(
            @PathVariable Long gymId,
            @PathVariable Long ratingId,
            @RequestBody ModerationRequest request
    ) {
        gymAdminWriteService.approveRating(gymId, ratingId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{gymId}/ratings/{ratingId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Rəyi rədd et", description = "Seçilmiş rəyi rədd edir.")
    public ResponseEntity<ApiResponse<Void>> rejectRating(
            @PathVariable Long gymId,
            @PathVariable Long ratingId,
            @RequestBody ModerationRequest request
    ) {
        gymAdminWriteService.rejectRating(gymId, ratingId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{gymId}/ratings/{ratingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Admin - Rəyi sil", description = "Seçilmiş rəyi sistemdən silir.")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long gymId,
            @PathVariable Long ratingId
    ) {
        gymAdminWriteService.deleteRating(gymId, ratingId);
        return ResponseEntity.noContent().build();
    }

}
