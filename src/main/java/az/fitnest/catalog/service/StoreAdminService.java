package az.fitnest.catalog.service;

import org.springframework.stereotype.Service;

@Service
public interface StoreAdminService {

    void updateStoreStatus(Long storeId, String status);
}
