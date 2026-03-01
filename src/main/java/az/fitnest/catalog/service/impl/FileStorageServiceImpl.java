/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.StorageGrpcClient;
import az.fitnest.catalog.dto.StorageFileData;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.service.FileStorageService;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl
        implements FileStorageService {
    private static final long MAX_FILE_SIZE = 0x500000L;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png");
    private final StorageGrpcClient storageGrpcClient;

    public FileStorageServiceImpl(StorageGrpcClient storageGrpcClient) {
        this.storageGrpcClient = storageGrpcClient;
    }

    @Override
    public String saveFile(MultipartFile file) {
        return this.saveFile(file, "/catalog");
    }

    @Override
    public String saveFile(MultipartFile file, String directory) {
        return this.saveFile(file, directory, null);
    }

    @Override
    public String saveFile(MultipartFile file, String directory, String oldPath) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        this.validateFile(file);
        try {
            String extractedOldPath = this.extractIdFromUrl(oldPath);
            StorageFileData data = this.storageGrpcClient.uploadFile(file, directory, extractedOldPath);
            return String.valueOf(data.getFsId());
        } catch (Exception e) {
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only JPEG and PNG images are allowed");
        }
        if (file.getSize() > 0x500000L) {
            throw new BadRequestException("File size exceeds maximum allowed size of 5MB");
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return;
        }
        this.deleteFiles(List.of(fileUrl));
    }

    @Override
    public void deleteFiles(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }
        try {
            List<String> ids = fileUrls.stream().map(this::extractIdFromUrl).filter(id -> id != null && !id.isBlank()).toList();
            if (!ids.isEmpty()) {
                this.storageGrpcClient.deleteFiles(ids);
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private String extractIdFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        if (url.contains("/")) {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        }
        return url;
    }
}

