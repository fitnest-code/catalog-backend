package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDeletionService {

    private final FileStorageService fileStorageService;

    @Async("fileDeletionExecutor")
    public void deleteFilesAsync(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        log.info("Starting asynchronous file deletion for {} files", urls.size());
        try {
            fileStorageService.deleteFiles(urls);
        } catch (Exception e) {
            log.error("Failed to delete files asynchronously", e);
        }
    }

    @Async("fileDeletionExecutor")
    public void deleteFileAsync(String url) {
        if (url == null || url.isBlank()) return;
        log.info("Starting asynchronous file deletion for file: {}", url);
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
            log.error("Failed to delete file asynchronously: {}", url, e);
        }
    }

    /**
     * Schedules file deletion after the current transaction successfully commits.
     */
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
}
