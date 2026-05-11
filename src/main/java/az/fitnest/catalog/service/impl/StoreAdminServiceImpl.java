package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.StorageGrpcClient;
import az.fitnest.catalog.dto.request.DiscountItemRequest;
import az.fitnest.catalog.dto.request.StoreStep2Request;
import az.fitnest.catalog.dto.request.StoreStep3Request;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.model.entity.StoreDiscount;
import az.fitnest.catalog.model.entity.StoreSocialLink;
import az.fitnest.catalog.model.entity.StoreWorkHours;
import az.fitnest.catalog.model.enums.StoreStatus;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.service.StoreAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class StoreAdminServiceImpl implements StoreAdminService {

    private static final String STORE_COVER_DIR = "stores/covers";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final StoreRepository storeRepository;
    private final StorageGrpcClient storageGrpcClient;


    @Override
    @Transactional
    public Long createMarketStep1(String name, MultipartFile photo) {

        var fileData = storageGrpcClient.uploadFile(photo, STORE_COVER_DIR);

        String coverUrl = storageGrpcClient.getDownloadUrl(String.valueOf(fileData.fsId()));

        Store store = Store.builder()
                .name(name)
                .status(StoreStatus.DRAFT.name())
                .coverImageUrl(coverUrl)
                .build();

        storeRepository.save(store);
        return store.getId();
    }


    @Override
    @Transactional
    public void createMarketStep2(Long id, StoreStep2Request request) {

        Store store = findById(id);

        Address address = store.getAddress() != null
                ? store.getAddress()
                : new Address();
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        store.setAddress(address);

        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());

        if (request.getSocialUrl() != null) {
            StoreSocialLink social = new StoreSocialLink();
            social.setUrl(request.getSocialUrl());
            store.setSocialLink(social);
        }

        if (request.getWorkHours() != null) {
            StoreWorkHours wh = new StoreWorkHours();
            wh.setFromTime(LocalTime.parse(request.getWorkHours().getFrom(), TIME_FMT));
            wh.setToTime(LocalTime.parse(request.getWorkHours().getTo(),   TIME_FMT));
            store.setWorkHours(wh);
        }

        storeRepository.save(store);
    }


    @Override
    @Transactional
    public void createMarketStep3(Long id, StoreStep3Request request) {

        Store store = findById(id);

        store.getDiscounts().clear();

        for (DiscountItemRequest item : request.getDiscounts()) {
            StoreDiscount discount = new StoreDiscount(
                    item.getPackageId(),
                    item.getDiscountPercent()
            );
            store.getDiscounts().add(discount);
        }

        store.setStatus(StoreStatus.ACTIVE.name());

        storeRepository.save(store);
    }


    private Store findById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
    }
}
