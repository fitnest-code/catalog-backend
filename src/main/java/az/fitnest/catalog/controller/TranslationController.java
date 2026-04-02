package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.ApiResponse;
import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.repository.TranslationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/translations")
@RequiredArgsConstructor
@Tag(name = "Translation Management", description = "Tərcümələri idarə etmək üçün ucluqlar")
public class TranslationController {
    private final TranslationRepository translationRepository;

    @Operation(summary = "Tərcümə yaradın və ya yeniləyin", description = "Verilmiş obyekt, dil və sahə üçün yeni tərcümə yaradır və ya mövcud olanı yeniləyir.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tərcümə uğurla yaradıldı/yeniləndi",
                    content = @Content(schema = @Schema(implementation = Translation.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Translation>> createOrUpdateTranslation(@RequestBody CreateTranslationRequest request) {
        String normalizedEntityType = request.entityType().toUpperCase();
        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, request.entityId(), request.languageCode().toUpperCase(), request.fieldName()
        ).orElse(null);

        if (existing != null) {
            existing.setFieldValue(request.fieldValue());
            existing.setEntityType(normalizedEntityType);
            Translation saved = translationRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.success(saved));
        } else {
            Translation translation = Translation.builder()
                    .entityType(normalizedEntityType)
                    .entityId(request.entityId())
                    .languageCode(request.languageCode().toUpperCase())
                    .fieldName(request.fieldName())
                    .fieldValue(request.fieldValue())
                    .build();
            Translation saved = translationRepository.save(translation);
            return ResponseEntity.ok(ApiResponse.success(saved));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTranslation(@PathVariable Long id) {
        if (!translationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        translationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Translation>> getTranslations(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String languageCode) {
        List<Translation> results = translationRepository.findAll().stream()
                .filter(t -> entityType == null || t.getEntityType().equals(entityType))
                .filter(t -> entityId == null || t.getEntityId().equals(entityId))
                .filter(t -> fieldName == null || t.getFieldName().equals(fieldName))
                .filter(t -> languageCode == null || t.getLanguageCode().equalsIgnoreCase(languageCode))
                .toList();
        return ResponseEntity.ok(results);
    }
}
