package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.request.LessonTypeRequest;
import az.fitnest.catalog.dto.response.LessonTypeResponse;
import az.fitnest.catalog.model.entity.LessonType;
import az.fitnest.catalog.repository.LessonTypeRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/lesson-types")
@RequiredArgsConstructor
@Tag(name = "Lesson Type Admin", description = "Qlobal Növləri idarə etmək üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
public class LessonTypeAdminController {

    private final LessonTypeRepository lessonTypeRepository;

    @Operation(summary = "Yeni növ yaradın", description = "Sistem üçün qlobal yeni bir növ (lesson type) yaradır.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonTypeResponse> createLessonType(@Valid @RequestBody LessonTypeRequest request) {
        LessonType lessonType = LessonType.builder()
                .name(request.getName())
                .build();
        lessonType = lessonTypeRepository.save(lessonType);
        return ResponseEntity.created(URI.create("/api/v1/admin/lesson-types/" + lessonType.getId()))
                .body(new LessonTypeResponse(lessonType.getId(), lessonType.getName()));
    }

    @Operation(summary = "Bütün növləri gətir", description = "Sistemdəki bütün növləri qaytarır.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LessonTypeResponse>> getAllLessonTypes() {
        List<LessonTypeResponse> responses = lessonTypeRepository.findAll().stream()
                .map(lt -> new LessonTypeResponse(lt.getId(), lt.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
