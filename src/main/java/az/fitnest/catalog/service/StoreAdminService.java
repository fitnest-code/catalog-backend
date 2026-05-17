package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.StoreStep2Request;
import az.fitnest.catalog.dto.request.StoreStep3Request;
import az.fitnest.catalog.dto.request.StoreUpdateRequest;
import az.fitnest.catalog.dto.response.AdminStoreDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface StoreAdminService {

    Long createMarketStep1(String name, MultipartFile photo);

    void createMarketStep2(Long id, StoreStep2Request request);

    void createMarketStep3(Long id, StoreStep3Request request);

    void updateStoreStatus(Long storeId, String status);

    az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.response.AdminStoreResponse> getAllStoresAdmin(String query, String sort, int page, int pageSize);

    void updateStore(Long id, StoreUpdateRequest request, MultipartFile photo);

    void deleteStore(Long id);

    AdminStoreDetailResponse getStoreById(Long id);

    void validateStep1(String name, MultipartFile photo);

    void validateStep2(StoreStep2Request request);

    void validateStep3(StoreStep3Request request);
}
