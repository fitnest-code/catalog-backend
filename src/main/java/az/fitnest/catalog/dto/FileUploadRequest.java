package az.fitnest.catalog.dto;

import lombok.Data;

@Data
public class FileUploadRequest {
    private String file; // Base64-encoded file content
}
