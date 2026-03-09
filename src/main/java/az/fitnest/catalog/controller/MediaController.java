package az.fitnest.catalog.controller;

import az.fitnest.catalog.client.StorageGrpcClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@Slf4j
@RequestMapping(value = {"/api/v1/media"})
@Tag(name = "Media", description = "Media məzmununu yayımlamaq üçün ucluqlar")
public class MediaController {
    private final StorageGrpcClient storageGrpcClient;

    public MediaController(StorageGrpcClient storageGrpcClient) {
        this.storageGrpcClient = storageGrpcClient;
    }

    @Operation(summary = "Media faylını yayımlayın", description = "Media faylını (şəkli) birbaşa yaddaşdan yayımlayır.")
    @GetMapping(value = {"/stream/{fsId}"}, produces = {"image/jpeg", "image/png", "application/octet-stream"})
    public ResponseEntity<StreamingResponseBody> streamFile(@Parameter(description = "Medianın fayl sistemi ID-si") @PathVariable String fsId) {
        log.info("[MediaController] stream request received for fsId={}", fsId);

        // Capture the current SecurityContext so it can be propagated into the streaming thread
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(outputStream -> {
                    SecurityContext previous = SecurityContextHolder.getContext();
                    try {
                        SecurityContextHolder.setContext(securityContext);

                        storageGrpcClient.downloadFile(fsId, response -> {
                            if (response.hasFileData()) {
                                try {
                                    outputStream.write(response.getFileData().toByteArray());
                                } catch (java.io.IOException e) {
                                    log.error("[MediaController] error writing chunk for fsId={}", fsId, e);
                                    throw new RuntimeException("Failed to stream file", e);
                                }
                            }
                        });

                        try {
                            outputStream.flush();
                        } catch (java.io.IOException e) {
                            log.warn("[MediaController] flush failed for fsId={}", fsId, e);
                        }

                    } finally {
                        SecurityContextHolder.setContext(previous);
                    }
                });
    }
}
