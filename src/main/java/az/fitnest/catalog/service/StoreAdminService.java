package az.fitnest.catalog.service;

import org.springframework.stereotype.Service;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;

@Service
public interface StoreAdminService {

    void updateStoreStatus(Long storeId, String status);

    az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.response.AdminStoreResponse> getAllStoresAdmin(String query, String sort, int page, int pageSize);
}
