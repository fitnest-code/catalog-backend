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

import java.util.List;

@RestController
@RequestMapping("/api/v1/professions")
@RequiredArgsConstructor
@Tag(name = "Profession API", description = "Məşqçi ixtisaslarına baxmaq üçün ucluqlar")
public class ProfessionController {

    private final ProfessionService professionService;

    @Operation(summary = "Bütün ixtisasları əldə edin", description = "Müəyyən edilmiş bütün ixtisasların siyahısını əldə edir. Hər kəs üçün əlçatandır.")
    @GetMapping
    public ResponseEntity<List<ProfessionDto>> getAllProfessions() {
        return ResponseEntity.ok(professionService.getAllProfessions());
    }

    @Operation(summary = "İxtisası ID vasitəsilə əldə edin")
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionDto> getProfessionById(@PathVariable Long id) {
        return ResponseEntity.ok(professionService.getProfessionById(id));
    }
}
