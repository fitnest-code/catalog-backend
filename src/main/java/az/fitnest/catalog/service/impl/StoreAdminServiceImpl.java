package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.service.StoreAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreAdminServiceImpl implements StoreAdminService {

    private final StoreRepository storeRepository;

    @Override
    public void updateStoreStatus(Long storeId, String status) {
        log.info("Updating store status. Store ID: {}, New Status: {}", storeId, status);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found with ID: " + storeId));
        store.setStatus(status);
        storeRepository.save(store);
        log.info("Store status updated successfully. Store ID: {}, New Status: {}", storeId, status);
    }
}
