package az.fitnest.catalog.controller;

import az.fitnest.catalog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping(value = {"/api/v1/media"})
@Tag(name = "Media", description = "Media məzmununu yayımlamaq üçün ucluqlar")
public class MediaController {
    private final FileStorageService fileStorageService;

    public MediaController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Media faylını yayımlayın", description = "Media faylını (şəkli) birbaşa yaddaşdan yayımlayır.")
    @GetMapping(value = {"/stream/{fsId}"})
    public ResponseEntity<StreamingResponseBody> streamFile(@Parameter(description = "Medianın fayl sistemi ID-si") @PathVariable String fsId) {

        SecurityContext securityContext = SecurityContextHolder.getContext();

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(outputStream -> {
                    SecurityContext previous = SecurityContextHolder.getContext();
                    try {
                        SecurityContextHolder.setContext(securityContext);

                        fileStorageService.streamFileToOutput(fsId, outputStream);

                        try {
                            outputStream.flush();
                        } catch (java.io.IOException e) {
                        }

                    } finally {
                        SecurityContextHolder.setContext(previous);
                    }
                });
    }
}
