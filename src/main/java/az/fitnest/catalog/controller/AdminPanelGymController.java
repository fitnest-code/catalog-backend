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

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Gyms", description = "Admin panelində idman zallarını idarə etmək və kəşf etmək üçün ucluqlar, o cümlədən paketlər, məşqçilər və istifadəçi rəyləri")
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
}
