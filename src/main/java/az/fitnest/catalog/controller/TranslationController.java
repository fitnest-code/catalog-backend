package az.fitnest.catalog.controller;

import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/translations")
@RequiredArgsConstructor
public class TranslationController {
    private final TranslationRepository translationRepository;

    @PostMapping
    public ResponseEntity<Translation> createTranslation(@RequestBody Translation translation) {
        Translation saved = translationRepository.save(translation);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Translation> updateTranslation(@PathVariable Long id, @RequestBody Translation translation) {
        Optional<Translation> existing = translationRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Translation toUpdate = existing.get();
        toUpdate.setEntityType(translation.getEntityType());
        toUpdate.setEntityId(translation.getEntityId());
        toUpdate.setLanguageCode(translation.getLanguageCode());
        toUpdate.setFieldName(translation.getFieldName());
        toUpdate.setFieldValue(translation.getFieldValue());
        Translation saved = translationRepository.save(toUpdate);
        return ResponseEntity.ok(saved);
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
