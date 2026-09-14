package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.StorageGrpcClient;
import az.fitnest.catalog.dto.response.StorageFileData;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.service.FileStorageService;
import io.grpc.StatusRuntimeException;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.OutputStream;

@Service
public class FileStorageServiceImpl
        implements FileStorageService {
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
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw mapUploadError(e);
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
        try {
            return new az.fitnest.catalog.util.ByteArrayMultipartFile(
                    file.getBytes(),
                    file.getName(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
        } catch (java.io.IOException e) {
            throw new BadRequestException("error.file_validation_failed");
        }
    }

    private static BadRequestException mapUploadError(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof StatusRuntimeException sre) {
                String desc = sre.getStatus().getDescription();
                if ("error.file_too_large".equals(desc) || "error.invalid_file_type".equals(desc) || "error.file_empty".equals(desc)) {
                    return new BadRequestException(desc);
                }
            }
        }
        return new BadRequestException("error.file_upload_failed");
    }
}
