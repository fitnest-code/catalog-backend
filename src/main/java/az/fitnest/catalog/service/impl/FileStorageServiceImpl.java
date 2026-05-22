package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.StorageGrpcClient;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.StorageFileData;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.service.FileStorageService;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.apache.tika.Tika;
import java.io.OutputStream;

@Service
public class FileStorageServiceImpl
        implements FileStorageService {
    private static final long MAX_FILE_SIZE = 0x500000L;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/x-png", "image/webp", "image/svg+xml", "image/svg"
    );
    private final StorageGrpcClient storageGrpcClient;
    private final Tika tika = new Tika();

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
            String extension = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String randomFilename = java.util.UUID.randomUUID() + extension;

            MultipartFile randomizedFile = new az.fitnest.catalog.util.ByteArrayMultipartFile(
                    file.getBytes(),
                    file.getName(),
                    randomFilename,
                    file.getContentType()
            );

            String extractedOldPath = this.extractIdFromUrl(oldPath);
            StorageFileData data = this.storageGrpcClient.uploadFile(randomizedFile, directory, extractedOldPath);
            return this.storageGrpcClient.getDownloadUrl(String.valueOf(data.fsId()));
        } catch (Exception e) {
            throw new BadRequestException("error.file_upload_failed");
        }
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("error.invalid_file_type");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("error.file_too_large");
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
        }
    }

    @Override
    @Async("fileDeletionExecutor")
    public void deleteFilesAsync(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        this.deleteFiles(urls);
    }

    @Override
    @Async("fileDeletionExecutor")
    public void deleteFileAsync(String url) {
        if (url == null || url.isBlank()) return;
        this.deleteFile(url);
    }

    @Override
    public void deleteFilesAfterCommit(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFilesAsync(urls);
                }
            });
        } else {
            deleteFilesAsync(urls);
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

    @Override
    public void streamFileToOutput(String fsId, OutputStream outputStream) {
        storageGrpcClient.downloadFile(fsId, response -> {
            if (response.hasFileData()) {
                try {
                    outputStream.write(response.getFileData().toByteArray());
                } catch (Exception e) {
                    throw new RuntimeException("error.file_upload_failed", e);
                }
            }
        });
    }

    @Override
    public MultipartFile validateAndWrapImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.file_empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("error.file_too_large");
        }
        try {
            byte[] bytes = file.getBytes();
            try (java.io.InputStream is = new java.io.ByteArrayInputStream(bytes)) {
                String mimeType = tika.detect(is);
                if (mimeType == null || !ALLOWED_CONTENT_TYPES.contains(mimeType.toLowerCase())) {
                    throw new BadRequestException("error.invalid_file_type");
                }
            }
            return new az.fitnest.catalog.util.ByteArrayMultipartFile(
                    bytes,
                    file.getName(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
        } catch (java.io.IOException e) {
            throw new BadRequestException("error.file_validation_failed");
        }
    }
}
