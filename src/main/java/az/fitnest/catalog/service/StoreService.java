/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.FilterResponseDto;
import az.fitnest.catalog.dto.StoreDetailResponseDto;
import az.fitnest.catalog.dto.StoreListResponseDto;
import az.fitnest.catalog.dto.StoreMainPageDto;
import az.fitnest.catalog.dto.StoreRequest;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.LocationDto;
import az.fitnest.catalog.dto.StoreSearchResponseDto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface StoreService {
    public PaginatedResponse<StoreMainPageDto> getStores(Long userId, String q, String type, Double lat, Double lng, int page, int pageSize);

    public StoreDetailResponseDto getStoreDetail(Long var1, Long var2);

    public FilterResponseDto getFilters();

    public StoreDetailResponseDto createStore(StoreRequest var1);

    public StoreDetailResponseDto updateStore(Long var1, StoreRequest var2);

    public void deleteStore(Long var1);


    public boolean toggleSave(Long var1, Long var2);

    public String uploadStoreImage(Long storeId, MultipartFile file);

    public LocationDto getStoreLocation(Long storeId);

    // Image Management operations
    public az.fitnest.catalog.model.entity.Store getStoreEntityById(Long storeId);

    public void deleteFileSafely(String url);

    public String uploadFileDirectly(Long storeId, MultipartFile file);

    public void updateStoreLogoUrl(Long storeId, String logoUrl);

    public void updateStoreCoverImageUrl(Long storeId, String coverImageUrl);

    public void deleteAllStores();
}

