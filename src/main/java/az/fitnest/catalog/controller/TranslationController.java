package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.ApiResponse;
import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.exception.ConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import az.fitnest.catalog.service.TranslationService;

@RestController
@RequestMapping("/api/v1/admin/translations")
@RequiredArgsConstructor
@Tag(name = "Translation Management", description = "Tərcümələri idarə etmək üçün ucluqlar")
public class TranslationController {
    private final TranslationService translationService;

    @Operation(summary = "Tərcümə yaradın", description = "Verilmiş obyekt, dil və sahə üçün yeni tərcümə yaradır.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tərcümə uğurla yaradıldı",
                    content = @Content(schema = @Schema(implementation = Translation.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tərcümə artıq mövcuddur")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Translation>> createTranslation(@RequestBody CreateTranslationRequest request) {
        Translation saved = translationService.createTranslation(request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTranslation(@PathVariable Long id) {
        translationService.deleteTranslation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Translation>> getTranslations(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String languageCode) {
        List<Translation> results = translationService.getTranslations(entityType, entityId, fieldName, languageCode);
        return ResponseEntity.ok(results);
    }
}
