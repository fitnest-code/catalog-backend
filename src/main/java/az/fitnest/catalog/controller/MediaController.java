/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  io.swagger.v3.oas.annotations.Parameter
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.ResponseEntity$BodyBuilder
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
 */
package az.fitnest.catalog.controller;

import az.fitnest.catalog.client.StorageGrpcClient;
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
@RequestMapping(value={"/api/v1/media"})
@Tag(name="Media", description="Media məzmununu yayımlamaq üçün ucluqlar")
public class MediaController {
    private final StorageGrpcClient storageGrpcClient;

    @Operation(summary="Media faylını yayımlayın", description="Media faylını (şəkli) birbaşa yaddaşdan yayımlayır.")
    @GetMapping(value={"/stream/{fsId}"}, produces={"image/jpeg", "image/png", "application/octet-stream"})
    public ResponseEntity<StreamingResponseBody> streamFile(@Parameter(description="Medianın fayl sistemi ID-si") @PathVariable String fsId) {
        return ((ResponseEntity.BodyBuilder)((ResponseEntity.BodyBuilder)ResponseEntity.ok().header("Content-Disposition", new String[]{"inline"})).header("Cache-Control", new String[]{"public, max-age=31536000, immutable"})).body(outputStream -> {
            this.storageGrpcClient.downloadFile(fsId, response -> {
                if (response.hasFileData()) {
                    try {
                        outputStream.write(response.getFileData().toByteArray());
                    }
                    catch (IOException e) {
                        throw new RuntimeException("Failed to stream file", e);
                    }
                }
            });
            try {
                outputStream.flush();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        });
    }

    public MediaController(StorageGrpcClient storageGrpcClient) {
        this.storageGrpcClient = storageGrpcClient;
    }
}

