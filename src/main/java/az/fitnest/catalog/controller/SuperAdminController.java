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
@Tag(name = "Super Admin", description = "Super-administrativ tapşırıqlar üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final GymWriteService gymWriteService;
    private final StoreService storeService;
    private final CategoryService categoryService;
    private final ProfessionService professionService;

    @DeleteMapping("/gyms/all")
    @Operation(summary = "Bütün idman zallarını silin", description = "Bütün idman zallarını və onlarla əlaqəli məlumatları həmişəlik silir. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bütün idman zalları uğurla silindi"),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<Void> deleteAllGyms() {
        gymWriteService.deleteAllGyms();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/stores/all")
    @Operation(summary = "Bütün mağazaları silin", description = "Bütün mağazaları və onlarla əlaqəli məlumatları həmişəlik silir. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bütün mağazalar uğurla silindi"),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<Void> deleteAllStores() {
        storeService.deleteAllStores();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/categories/all")
    @Operation(summary = "Bütün kateqoriyaları silin", description = "Bütün kateqoriyaları həmişəlik silir. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bütün kateqoriyalar uğurla silindi"),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<Void> deleteAllCategories() {
        categoryService.deleteAllCategories();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/professions/all")
    @Operation(summary = "Bütün ixtisasları silin", description = "Bütün ixtisasları həmişəlik silir. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bütün ixtisaslar uğurla silindi"),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<Void> deleteAllProfessions() {
        professionService.deleteAllProfessions();
        return ResponseEntity.noContent().build();
    }
}
