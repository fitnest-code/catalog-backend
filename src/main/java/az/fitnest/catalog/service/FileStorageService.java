package az.fitnest.catalog.service;

import java.io.OutputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    public String saveFile(MultipartFile var1);

    public String saveFile(MultipartFile var1, String var2);

    public String saveFile(MultipartFile var1, String var2, String var3);

    void deleteFile(String var1);

    void deleteFiles(List<String> var1);

    void deleteFilesAsync(List<String> urls);

    void deleteFileAsync(String url);

    void deleteFilesAfterCommit(List<String> urls);

    void streamFileToOutput(String fsId, OutputStream outputStream);

    MultipartFile validateAndWrapImage(MultipartFile file);
}
