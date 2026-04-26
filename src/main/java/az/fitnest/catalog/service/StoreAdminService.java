package az.fitnest.catalog.service;

import org.springframework.stereotype.Service;

@Service
public interface StoreAdminService {

    void updateStoreStatus(Long storeId, String status);

    az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.AdminStoreResponse> getAllStoresAdmin(String query, String sort, int page, int pageSize);
}
