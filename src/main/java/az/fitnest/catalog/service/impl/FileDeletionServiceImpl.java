package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileDeletionServiceImpl implements az.fitnest.catalog.service.FileDeletionService {

    private final FileStorageService fileStorageService;

    @Async("fileDeletionExecutor")
    public void deleteFilesAsync(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        try {
            fileStorageService.deleteFiles(urls);
        } catch (Exception e) {
            // Deletion failed
        }
    }

    @Async("fileDeletionExecutor")
    public void deleteFileAsync(String url) {
        if (url == null || url.isBlank()) return;
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
            // Deletion failed
        }
    }

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
