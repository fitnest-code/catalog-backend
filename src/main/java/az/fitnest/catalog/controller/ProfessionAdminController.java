package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.ProfessionDto;
import az.fitnest.catalog.dto.ProfessionRequest;
import az.fitnest.catalog.service.impl.ProfessionService;
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
@RequestMapping("/api/v1/admin/professions")
@RequiredArgsConstructor
@Tag(name = "Profession Admin API", description = "Məşqçi ixtisaslarının administrativ idarə olunması üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class ProfessionAdminController {

    private final ProfessionService professionService;

    @Operation(summary = "İxtisas yaradın", description = "Yalnız Admin. Məşqçilər üçün yeni ixtisas yaradır.")
    @PostMapping
    public ResponseEntity<ProfessionDto> createProfession(@Valid @RequestBody ProfessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(professionService.createProfession(request));
    }

    @Operation(summary = "İxtisası yeniləyin", description = "Yalnız Admin. Mövcud ixtisası yeniləyir.")
    @PutMapping("/{id}")
    public ResponseEntity<ProfessionDto> updateProfession(@PathVariable Long id, @Valid @RequestBody ProfessionRequest request) {
        return ResponseEntity.ok(professionService.updateProfession(id, request));
    }

    @Operation(summary = "İxtisası silin", description = "Yalnız Admin. İxtisası silir.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfession(@PathVariable Long id) {
        professionService.deleteProfession(id);
        return ResponseEntity.noContent().build();
    }
}
