package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.request.LessonTypeRequest;
import az.fitnest.catalog.dto.response.LessonTypeResponse;
import az.fitnest.catalog.service.LessonTypeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/lesson-types")
@RequiredArgsConstructor
@Tag(name = "Lesson Type Admin", description = "Qlobal Növləri idarə etmək üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
public class LessonTypeAdminController {

    private final LessonTypeAdminService lessonTypeAdminService;

    @Operation(summary = "Yeni növ yaradın", description = "Sistem üçün qlobal yeni bir növ (lesson type) yaradır.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonTypeResponse> createLessonType(@Valid @RequestBody LessonTypeRequest request) {
        LessonTypeResponse response = lessonTypeAdminService.createLessonType(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/lesson-types/" + response.id()))
                .body(response);
    }

    @Operation(summary = "Bütün növləri gətir", description = "Sistemdəki bütün növləri qaytarır.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LessonTypeResponse>> getAllLessonTypes() {
        return ResponseEntity.ok(lessonTypeAdminService.getAllLessonTypes());
    }

    @Operation(summary = "Növü ID-yə görə gətir", description = "Verilmiş ID-yə əsasən növü qaytarır.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonTypeResponse> getLessonTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonTypeAdminService.getLessonTypeById(id));
    }

    @Operation(summary = "Növü yeniləyin", description = "Verilmiş ID-yə əsasən növün adını yeniləyir.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonTypeResponse> updateLessonType(
            @PathVariable Long id,
            @Valid @RequestBody LessonTypeRequest request) {
        return ResponseEntity.ok(lessonTypeAdminService.updateLessonType(id, request));
    }

    @Operation(summary = "Növü sil", description = "Verilmiş ID-yə əsasən növü sistemdən silir.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLessonType(@PathVariable Long id) {
        lessonTypeAdminService.deleteLessonType(id);
        return ResponseEntity.noContent().build();
    }
}
