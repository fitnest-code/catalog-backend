package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.AdminStoreResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.service.StoreAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<AdminStoreResponse> getAllStoresAdmin(String query, String sort, int page, int pageSize) {
        Sort springSort = Sort.unsorted();
        if (sort != null) {
            switch (sort) {
                case "name_asc":
                    springSort = Sort.by(Sort.Direction.ASC, "name");
                    break;
                case "name_desc":
                    springSort = Sort.by(Sort.Direction.DESC, "name");
                    break;
                case "address_asc":
                    springSort = Sort.by(Sort.Direction.ASC, "address.city", "address.addressText");
                    break;
                case "newest":
                    springSort = Sort.by(Sort.Direction.DESC, "createdDate");
                    break;
                default:
                    springSort = Sort.by(Sort.Direction.DESC, "createdDate");
            }
        } else {
            springSort = Sort.by(Sort.Direction.DESC, "createdDate");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, springSort);

        Specification<Store> spec = (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("address").get("addressText")), pattern),
                        cb.like(cb.lower(root.get("address").get("city")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Store> storePage = storeRepository.findAll(spec, pageable);

        List<AdminStoreResponse> items = storePage.getContent().stream().map(store -> {
            String fullAddress = (store.getAddress() != null)
                    ? (store.getAddress().getCity() + ", " + store.getAddress().getAddressText())
                    : "N/A";

            return AdminStoreResponse.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .fullAddress(fullAddress)
                    .phone(store.getPhone())
                    .status(store.getStatus())
                    .build();
        }).collect(Collectors.toList());

        return PaginatedResponse.<AdminStoreResponse>builder()
                .items(items)
                .total(storePage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }
}
