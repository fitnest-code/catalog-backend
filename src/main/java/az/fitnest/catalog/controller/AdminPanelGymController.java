package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.dto.admin.AdminGymListDto;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.service.impl.AdminPanelGymReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gyms")
@RequiredArgsConstructor
@Tag(name = "Gyms", description = "Admin panelində idman zallarını idarə etmək və kəşf etmək üçün ucluqlar, o cümlədən paketlər, məşqçilər və istifadəçi rəyləri")
@SecurityRequirement(name = "bearerAuth")
public class AdminPanelGymController {

    private final AdminPanelGymReadService gymAdminService;

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
        return ResponseEntity.ok(gymAdminService.getGymsForAdmin(
                search, status, cityId, districtId,
                sortBy, sortOrder, page, safeSize
        ));
    }

}
