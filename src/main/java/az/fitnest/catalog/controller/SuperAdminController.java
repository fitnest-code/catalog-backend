package az.fitnest.catalog.controller;

import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.service.StoreService;
import az.fitnest.catalog.service.impl.CategoryService;
import az.fitnest.catalog.service.impl.GymWriteService;
import az.fitnest.catalog.service.impl.ProfessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Endpoints for super-administrative tasks")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final GymWriteService gymWriteService;
    private final StoreService storeService;
    private final CategoryService categoryService;
    private final ProfessionService professionService;

    @DeleteMapping("/gyms/all")
    @Operation(summary = "Delete all gyms", description = "Permanently deletes all gyms and associated data. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All gyms deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteAllGyms() {
        gymWriteService.deleteAllGyms();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/stores/all")
    @Operation(summary = "Delete all stores", description = "Permanently deletes all stores and associated data. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All stores deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteAllStores() {
        storeService.deleteAllStores();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/categories/all")
    @Operation(summary = "Delete all categories", description = "Permanently deletes all categories. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All categories deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteAllCategories() {
        categoryService.deleteAllCategories();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/professions/all")
    @Operation(summary = "Delete all professions", description = "Permanently deletes all professions. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All professions deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteAllProfessions() {
        professionService.deleteAllProfessions();
        return ResponseEntity.noContent().build();
    }
}
