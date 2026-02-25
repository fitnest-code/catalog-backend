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
import az.fitnest.catalog.dto.StoreResponseDto;
import az.fitnest.catalog.dto.LocationDto;
import az.fitnest.catalog.dto.StoreSearchResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface StoreService {
    public StoreListResponseDto getStores(Long var1, int var2, int var3);

    public StoreDetailResponseDto getStoreDetail(Long var1, Long var2);

    public FilterResponseDto getFilters();

    public StoreDetailResponseDto createStore(StoreRequest var1);

    public StoreDetailResponseDto updateStore(Long var1, StoreRequest var2);

    public void deleteStore(Long var1);

    public List<StoreMainPageDto> getMainPageStores();

    public String uploadStoreImage(Long var1, MultipartFile var2);

    public StoreResponseDto getStoreMainPage(Long var1, String var2, int var3, int var4);

    public StoreResponseDto getClosestStores(Long var1, String var2, int var3, int var4, Double var5, Double var6);

    public List<StoreMainPageDto> getDiscountedStores(Long var1);

    public List<StoreMainPageDto> getDiscountedStores(Long var1, String var2);

    public StoreResponseDto getDiscountedStores(Long var1, String var2, int var3, int var4);

    public StoreListResponseDto getAllStores(Long var1);

    public List<StoreMainPageDto> getSavedStores(Long var1);

    public boolean toggleSave(Long var1, Long var2);

    public StoreSearchResponseDto searchStoresForQuery(Long var1, String var2, int var3, int var4);

    public StoreResponseDto getNewStores(Long var1, String var2, int var3, int var4);

    public LocationDto getStoreLocation(Long storeId);

    // Image Management operations
    public az.fitnest.catalog.model.entity.Store getStoreEntityById(Long storeId);
    public void deleteFileSafely(String url);
    public String uploadFileDirectly(Long storeId, MultipartFile file);
    public void updateStoreLogoUrl(Long storeId, String logoUrl);
    public void updateStoreCoverImageUrl(Long storeId, String coverImageUrl);
}

