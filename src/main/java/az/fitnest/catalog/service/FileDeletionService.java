package az.fitnest.catalog.service;

import java.util.List;

public interface FileDeletionService {
    void deleteFilesAsync(List<String> urls);
    void deleteFileAsync(String url);
    void deleteFilesAfterCommit(List<String> urls);
}
