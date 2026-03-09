package az.fitnest.catalog.controller;

import az.fitnest.catalog.client.StorageGrpcClient;
import az.fitnest.catalog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping(value = {"/api/v1/media"})
@Tag(name = "Media", description = "Media məzmununu yayımlamaq üçün ucluqlar")
public class MediaController {
    private final StorageGrpcClient storageGrpcClient;
    private final FileStorageService fileStorageService;

    public MediaController(StorageGrpcClient storageGrpcClient, FileStorageService fileStorageService) {
        this.storageGrpcClient = storageGrpcClient;
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Media faylını yayımlayın", description = "Media faylını (şəkli) birbaşa yaddaşdan yayımlayır.")
    @GetMapping(value = {"/stream/{fsId}"}, produces = {"image/jpeg", "image/png", "application/octet-stream"})
    public ResponseEntity<StreamingResponseBody> streamFile(@Parameter(description = "Medianın fayl sistemi ID-si") @PathVariable String fsId) {
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(outputStream -> fileStorageService.streamFileToOutput(fsId, outputStream));
    }
}
