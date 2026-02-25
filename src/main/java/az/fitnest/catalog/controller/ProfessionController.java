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
@Tag(name = "Profession API", description = "Endpoints for viewing trainer professions")
public class ProfessionController {

    private final ProfessionService professionService;

    @Operation(summary = "Get all professions", description = "Retrieves a list of all defined professions. Publicly accessible.")
    @GetMapping
    public ResponseEntity<List<ProfessionDto>> getAllProfessions() {
        return ResponseEntity.ok(professionService.getAllProfessions());
    }

    @Operation(summary = "Get profession by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionDto> getProfessionById(@PathVariable Long id) {
        return ResponseEntity.ok(professionService.getProfessionById(id));
    }
}
