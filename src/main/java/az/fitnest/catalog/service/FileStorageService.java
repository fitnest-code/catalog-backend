/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    public String saveFile(MultipartFile var1);

    public String saveFile(MultipartFile var1, String var2);

    public String saveFile(MultipartFile var1, String var2, String var3);

    public void deleteFile(String var1);

    public void deleteFiles(List<String> var1);
}

