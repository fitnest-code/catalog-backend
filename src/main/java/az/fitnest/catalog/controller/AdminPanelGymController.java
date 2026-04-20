package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.ApiResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.service.impl.AdminPanelGymReadService;
import az.fitnest.catalog.service.impl.AdminPanelGymWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "Admin - Zalların siyahısı")
    public ResponseEntity<PaginatedResponse<AdminGymListDto>> getGymsForAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) GymStatus status,
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
    public ResponseEntity<ApiResponse<AdminPanelGymResponse>> createGymForAdmin(
            @RequestBody @Valid AdminPanelCreateGymRequest request
    ) {
        AdminPanelGymResponse response = gymAdminWriteService.createGymForAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{gymId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AdminPanelGymDetailDto>> getGymForAdmin(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getGymForAdmin(gymId)));
    }

    @DeleteMapping("/{gymId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteGym(@PathVariable Long gymId) {
        gymAdminWriteService.deleteGym(gymId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/general-info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateGeneralInfo(
            @PathVariable Long gymId,
            @RequestBody @Valid GeneralInfoRequest request
    ) {
        gymAdminWriteService.updateGeneralInfo(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{gymId}/cover-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CoverImageResponse>> uploadCoverImage(
            @PathVariable Long gymId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminWriteService.uploadCoverImage(gymId, file)));
    }

    @DeleteMapping("/{gymId}/cover-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteCoverImage(@PathVariable Long gymId) {
        gymAdminWriteService.deleteCoverImage(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gymId}/gallery-images")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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
    public ResponseEntity<ApiResponse<List<AdminPanelGymImageDto>>> getGalleryImages(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getGalleryImages(gymId)));
    }

    @DeleteMapping("/{gymId}/gallery-images/{imageId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteGalleryImage(
            @PathVariable Long gymId,
            @PathVariable Long imageId
    ) {
        gymAdminWriteService.deleteGalleryImage(gymId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/gallery-images/order")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateImageOrder(
            @PathVariable Long gymId,
            @RequestBody @Valid UpdateImageOrderRequest request
    ) {
        gymAdminWriteService.updateImageOrder(gymId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{gymId}/working-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<WorkingHourDto>>> getWorkingHours(
            @PathVariable Long gymId
    ) {
        return ResponseEntity.ok(ApiResponse.success(gymAdminReadService.getWorkingHours(gymId)));
    }

    @PostMapping("/{gymId}/working-hours")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<WorkingHourDto>> addWorkingHour(
            @PathVariable Long gymId,
            @RequestBody @Valid WorkingHourRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(gymAdminWriteService.addWorkingHour(gymId, request)));
    }

    @PutMapping("/{gymId}/working-hours/{workingHourId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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
    public ResponseEntity<Void> deleteWorkingHour(
            @PathVariable Long gymId,
            @PathVariable Long workingHourId
    ) {
        gymAdminWriteService.deleteWorkingHour(gymId, workingHourId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gymId}/trainers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PaginatedResponse<TrainerListDto>> getTrainers(
            @PathVariable Long gymId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(gymAdminReadService.getTrainers(gymId, search, page, Math.min(size, 100)));
    }

}
