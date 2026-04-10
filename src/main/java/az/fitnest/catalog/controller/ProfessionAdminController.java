package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.ProfessionDto;
import az.fitnest.catalog.dto.ProfessionRequest;
import az.fitnest.catalog.service.impl.ProfessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/professions")
@RequiredArgsConstructor
@Tag(name = "Profession Admin", description = "İxtisasları idarə etmək üçün administrativ ucluqlar.")
@SecurityRequirement(name = "bearerAuth")
public class ProfessionAdminController {

    private final ProfessionService professionService;

    @Operation(summary = "Yeni ixtisas yaradın", description = "Sistemdə yeni bir məşqçi ixtisası yaradır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "İxtisas uğurla yaradıldı"),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu və ya ixtisas artıq mövcuddur")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfessionDto> createProfession(@Valid @RequestBody ProfessionRequest request) {
        ProfessionDto created = professionService.createProfession(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/professions/" + created.id())).body(created);
    }

    @Operation(summary = "İxtisası yeniləyin", description = "Mövcud olan ixtisas məlumatlarını ID vasitəsilə yeniləyir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İxtisas uğurla yeniləndi"),
            @ ApiResponse(responseCode = "404", description = "İxtisas tapılmadı")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfessionDto> updateProfession(@PathVariable Long id, @Valid @RequestBody ProfessionRequest request) {
        return ResponseEntity.ok(professionService.updateProfession(id, request));
    }

    @Operation(summary = "İxtisası silin", description = "Mövcud olan ixtisası ID vasitəsilə sistemdən silir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "İxtisas uğurla silindi"),
            @ApiResponse(responseCode = "404", description = "İxtisas tapılmadı")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProfession(@PathVariable Long id) {
        professionService.deleteProfession(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Bütün ixtisasları silin (Kritik)", description = "Sistemdəki BÜTÜN ixtisasları silir. Bu əməliyyat üçün SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bütün ixtisaslar uğurla silindi")
    })
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAllProfessions() {
        professionService.deleteAllProfessions();
        return ResponseEntity.noContent().build();
    }
}
