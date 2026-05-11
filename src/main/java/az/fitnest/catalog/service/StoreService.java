package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.request.AddDiscountRequest;
import az.fitnest.catalog.dto.request.StoreRequest;
import az.fitnest.catalog.dto.response.FilterResponse;
import az.fitnest.catalog.dto.response.LocationResponse;
import az.fitnest.catalog.dto.response.StoreDetailResponse;
import az.fitnest.catalog.dto.response.StoreMainPageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StoreService {
    public PaginatedResponse<StoreMainPageResponse> getStores(Long userId, String q, String type, Double lat, Double lng, int page, int pageSize, String sortDir);

    public StoreDetailResponse getStoreDetail(Long var1, Long var2);

    public FilterResponse getFilters();

    public StoreDetailResponse createStore(StoreRequest var1);

    public StoreDetailResponse updateStore(Long var1, StoreRequest var2);

    public void deleteStore(Long var1);

    void addDiscount(Long storeId, AddDiscountRequest request);

    public boolean toggleSave(Long var1, Long var2);

    public LocationResponse getStoreLocation(Long storeId);

    public az.fitnest.catalog.model.entity.Store getStoreEntityById(Long storeId);

    public void deleteFileSafely(String url);

    public String uploadFileDirectly(Long storeId, MultipartFile file);

    public void updateStoreLogoUrl(Long storeId, String logoUrl);

    public void updateStoreCoverImageUrl(Long storeId, String coverImageUrl);

    public void deleteAllStores();

    public String uploadStoreImage(Long storeId, MultipartFile file);

}
