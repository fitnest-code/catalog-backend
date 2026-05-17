package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;
public record StorageFileData(
    String path,
    long size,
    String md5,
    long fsId
) {}
