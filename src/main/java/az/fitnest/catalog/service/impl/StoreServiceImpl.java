package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.repository.SavedStoreRepository;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.service.StoreService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {
    private final StoreRepository storeRepository;
    private final SavedStoreRepository savedStoreRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final FileStorageService fileStorageService;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<StoreMainPageDto> getStores(Long userId, String q, String type, Double lat, Double lng, int page, int pageSize, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(direction, "createdDate"));
        Page<Store> storePage;

        if ("SAVED".equalsIgnoreCase(type)) {
            if (userId == null) return emptyResponse(page, pageSize);
            List<SavedStore> saved = savedStoreRepository.findByUserId(userId);
            List<Store> candidates = saved.stream().map(SavedStore::getStore).toList();
            return manualPaginate(candidates, userId, lat, lng, page, pageSize, q);
        }

        if (lat != null && lng != null && "CLOSEST".equalsIgnoreCase(type)) {
            double[] bbox = boundingBox(lat, lng, 50.0);
            storePage = storeRepository.findByAddressLatitudeBetweenAndAddressLongitudeBetween(bbox[0], bbox[1], bbox[2], bbox[3], pageable);
            if (q != null && !q.isBlank()) {
                return manualPaginate(storePage.getContent(), userId, lat, lng, page, pageSize, q);
            }
        } else if ("DISCOUNTED".equalsIgnoreCase(type)) {
            storePage = (q != null && !q.isBlank()) ? storeRepository.findDiscountedStoresByQuery("%" + q.toLowerCase() + "%", pageable) : storeRepository.findDiscountedStores(pageable);
        } else if ("NEW".equalsIgnoreCase(type)) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            storePage = (q != null && !q.isBlank()) ? storeRepository.findNewStoresByQuery(cutoff, "%" + q.toLowerCase() + "%", pageable) : storeRepository.findNewStores(cutoff, pageable);
        } else {
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                storePage = storeRepository.findAll((Specification<Store> & Serializable) (root, query, cb) ->
                        cb.or(cb.like(cb.lower(root.get("name")), pattern), cb.like(cb.lower(root.get("address").get("addressText")), pattern)), pageable);
            } else {
                storePage = storeRepository.findAllWithAssociations(pageable);
            }
        }

        List<StoreMainPageDto> items = storePage.getContent().stream()
                .map(s -> mapToSummary(s, userId, lat, lng))
                .collect(Collectors.toList());

        return PaginatedResponse.<StoreMainPageDto>builder()
                .items(items)
                .total(storePage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private PaginatedResponse<StoreMainPageDto> manualPaginate(List<Store> candidates, Long userId, Double lat, Double lng, int page, int pageSize, String q) {
        Stream<Store> stream = candidates.stream();
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stream = stream.filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(lowerQ)) ||
                    (s.getAddress() != null && s.getAddress().getAddressText() != null && s.getAddress().getAddressText().toLowerCase().contains(lowerQ)));
        }

        List<StoreMainPageDto> all = stream.map(s -> mapToSummary(s, userId, lat, lng)).collect(Collectors.toList());

        if (lat != null && lng != null) {
            all.sort(Comparator.comparing(StoreMainPageDto::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<StoreMainPageDto> pageItems = from >= all.size() ? new ArrayList<>() : new ArrayList<>(all.subList(from, to));

        return PaginatedResponse.<StoreMainPageDto>builder()
                .items(pageItems)
                .total(all.size())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    private PaginatedResponse<StoreMainPageDto> emptyResponse(int page, int pageSize) {
        return PaginatedResponse.<StoreMainPageDto>builder().items(Collections.emptyList()).total(0).page(page).pageSize(pageSize).build();
    }

    private String getUserLanguage(Long userId) {
        String language = "AZ";
        if (userId != null) {
            try {
                az.fitnest.user.grpc.UserResponse user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                    language = user.getLanguage();
                }
            } catch (Exception ignored) {}
        }
        return language;
    }

    private StoreMainPageDto mapToSummary(Store store, Long userId, Double lat, Double lng) {
        Double distance = null;
        if (lat != null && lng != null && store.getAddress() != null && store.getAddress().getLatitude() != null && store.getAddress().getLongitude() != null) {
            distance = calculateDistance(lat, lng, store.getAddress().getLatitude(), store.getAddress().getLongitude());
        }

        boolean isSaved = false;
        if (userId != null) {
            isSaved = !savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, List.of(store.getId())).isEmpty();
        }

        String userLanguage = getUserLanguage(userId);
        String localizedName = translationService.getTranslatedValue("STORE", store.getId().toString(), "name", userLanguage);
        if (localizedName == null || localizedName.isEmpty()) localizedName = store.getName();

        return StoreMainPageDto.builder()
                .storeId(store.getId())
                .name(localizedName)
                .address(store.getAddress() != null ? getLocalizedAddressField(store.getId(), "STORE", store.getAddress(), "addressText", userLanguage) : null)
                .city(store.getAddress() != null ? getLocalizedAddressField(store.getId(), "STORE", store.getAddress(), "city", userLanguage) : null)
                .logoUrl(store.getLogoUrl())
                .coverImageUrl(store.getCoverImageUrl())
                .discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).toList() : Collections.emptyList())
                .isSaved(isSaved)
                .distanceKm(distance)
                .social(store.getSocialLink() != null ? StoreSocialDto.builder().name(store.getSocialLink().getName()).url(store.getSocialLink().getUrl()).build() : null)
                .isNew(isNew(store))
                .build();
    }

    private boolean isNew(Store store) {
        return store != null && store.getCreatedDate() != null && store.getCreatedDate().isAfter(LocalDateTime.now().minusDays(30));
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDetailResponseDto getStoreDetail(Long userId, Long storeId) {
        Store store = storeRepository.findByIdWithAssociations(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
        boolean isSaved = false;
        if (userId != null) {
            isSaved = !savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, List.of(store.getId())).isEmpty();
        }
        String userLanguage = getUserLanguage(userId);
        String localizedName = translationService.getTranslatedValue("STORE", store.getId().toString(), "name", userLanguage);
        if (localizedName == null || localizedName.isEmpty()) localizedName = store.getName();
        return StoreDetailResponseDto.builder()
                .storeId(store.getId())
                .name(localizedName)
                .address(store.getAddress() != null ? AddressDto.builder()
                        .addressText(getLocalizedAddressField(store.getId(), "STORE", store.getAddress(), "addressText", userLanguage))
                        .city(getLocalizedAddressField(store.getId(), "STORE", store.getAddress(), "city", userLanguage))
                        .latitude(store.getAddress().getLatitude())
                        .longitude(store.getAddress().getLongitude())
                        .build() : null)
                .phone(store.getPhone())
                .category(store.getCategory())
                .status(store.getStatus())

                .discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).toList() : Collections.emptyList())
                .social(store.getSocialLink() != null ? StoreSocialDto.builder().name(store.getSocialLink().getName()).url(store.getSocialLink().getUrl()).build() : null)
                .images(store.getImages() != null ? store.getImages().stream().map(StoreImage::getUrl).toList() : Collections.emptyList())
                .isSaved(isSaved)
                .isNew(isNew(store))
                .build();
    }

    @Override
    @Transactional
    public boolean toggleSave(Long userId, Long storeId) {
        Optional<SavedStore> existing = savedStoreRepository.findByUserIdAndStoreId(userId, storeId);
        if (existing.isPresent()) {
            savedStoreRepository.delete(existing.get());
            return false;
        } else {
            Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
            SavedStore saved = new SavedStore();
            saved.setUserId(userId);
            saved.setStore(store);
            savedStoreRepository.save(saved);
            return true;
        }
    }

    @Override
    public FilterResponseDto getFilters() {
        return FilterResponseDto.builder()
                .tabs(List.of(new FilterResponseDto.TabDto("all", "All"), new FilterResponseDto.TabDto("popular", "Popular"),
                        new FilterResponseDto.TabDto("nearby", "Near you"), new FilterResponseDto.TabDto("new", "New")))
                .sortOptions(List.of("popular", "newest", "distance", "discount_desc", "name_asc"))
                .defaultRadiusKm(20)
                .build();
    }

    @Override
    @Transactional
    public StoreDetailResponseDto createStore(StoreRequest request) {
        Store store = new Store();
        updateStoreFromRequest(store, request);
        Store saved = storeRepository.save(store);
        return getStoreDetail(null, saved.getId());
    }

    @Override
    @Transactional
    public StoreDetailResponseDto updateStore(Long storeId, StoreRequest request) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
        updateStoreFromRequest(store, request);
        Store saved = storeRepository.save(store);
        return getStoreDetail(null, saved.getId());
    }

    @Override
    @Transactional
    public void deleteStore(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
        savedStoreRepository.deleteByStoreId(storeId);
        if (store.getLogoUrl() != null) fileStorageService.deleteFile(store.getLogoUrl());
        if (store.getCoverImageUrl() != null) fileStorageService.deleteFile(store.getCoverImageUrl());
        if (store.getImages() != null && !store.getImages().isEmpty()) {
            fileStorageService.deleteFiles(store.getImages().stream().map(StoreImage::getUrl).toList());
        }

        try {
            storeRepository.deleteStoreSocialLinksByStoreId(storeId);
        } catch (Exception ignored) {
        }

        storeRepository.delete(store);
    }

    private void updateStoreFromRequest(Store store, StoreRequest request) {
        store.setName(request.name());
        Double reqLat = request.address() != null ? request.address().latitude() : null;
        Double reqLng = request.address() != null ? request.address().longitude() : null;

        boolean coordsChanged = store.getAddress() == null || !Objects.equals(store.getAddress().getLatitude(), reqLat) || !Objects.equals(store.getAddress().getLongitude(), reqLng);

        GeocodingResponse geocoding = (store.getAddress() != null && !coordsChanged)
                ? GeocodingResponse.builder().addressText(store.getAddress().getAddressText()).city(store.getAddress().getCity()).build()
                : resolveGeocoding(reqLat, reqLng);

        store.setAddress(request.address() != null ? new StoreAddress(geocoding.addressText(), geocoding.city(), reqLat, reqLng) : null);

        store.setPhone(request.phone());
        store.setCategory(request.category());
        store.setStatus(request.status());

        if (request.social() != null) {
            store.setSocialLink(new StoreSocialLink(request.social().name(), request.social().url()));
        } else {
            store.setSocialLink(null);
        }
    }

    private GeocodingResponse resolveGeocoding(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return GeocodingResponse.builder().build();
        return reverseGeocodingService.reverseGeocode(latitude, longitude);
    }

    private Double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (double) Math.round(R * c * 10.0) / 10.0;
    }

    private double[] boundingBox(double lat, double lng, double radiusKm) {
        double latRadians = Math.toRadians(lat);
        double radiusRatio = radiusKm / 6371.0;
        double minLat = lat - Math.toDegrees(radiusRatio);
        double maxLat = lat + Math.toDegrees(radiusRatio);
        double deltaLng = Math.toDegrees(Math.asin(Math.sin(radiusRatio) / Math.cos(latRadians)));
        return new double[]{minLat, maxLat, lng - deltaLng, lng + deltaLng};
    }

    @Override
    @Transactional
    public String uploadStoreImage(Long storeId, MultipartFile file) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
        String fsId = fileStorageService.saveFile(file, "/stores/" + storeId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        StoreImage image = new StoreImage();
        image.setUrl(fullUrl);
        image.setType("gallery");
        if (store.getImages() == null) store.setImages(new LinkedHashSet<>());
        store.getImages().add(image);
        storeRepository.save(store);
        return fullUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDto getStoreLocation(Long storeId) {
        Store store = getStoreEntityById(storeId);
        StoreAddress addr = store.getAddress();
        if (addr == null) return LocationDto.builder().build();
        return LocationDto.builder().addressText(addr.getAddressText()).latitude(addr.getLatitude()).longitude(addr.getLongitude()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public Store getStoreEntityById(Long storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
    }

    @Override
    public void deleteFileSafely(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String uploadFileDirectly(Long storeId, MultipartFile file) {
        return "/api/v1/media/stream/" + fileStorageService.saveFile(file, "/stores/" + storeId);
    }

    @Override
    @Transactional
    public void updateStoreLogoUrl(Long storeId, String logoUrl) {
        Store store = getStoreEntityById(storeId);
        store.setLogoUrl(logoUrl);
        storeRepository.save(store);
    }

    @Override
    @Transactional
    public void updateStoreCoverImageUrl(Long storeId, String coverImageUrl) {
        Store store = getStoreEntityById(storeId);
        store.setCoverImageUrl(coverImageUrl);
        storeRepository.save(store);
    }

    @Override
    @Transactional
    public void deleteAllStores() {
        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            deleteStore(store.getId());
        }
    }

    @Override
    @Transactional
    public void addDiscount(Long storeId, AddDiscountRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));

        boolean exists = orderServiceGrpcClient.checkPackageExists(request.packageId());
        if (!exists) {
            throw new ResourceNotFoundException("PACKAGE_NOT_FOUND", "error.package_not_found");
        }

        StoreDiscount discount = new StoreDiscount(request.percent(), request.packageId().toString());
        store.getDiscounts().add(discount);
        storeRepository.save(store);
    }

    private String getLocalizedAddressField(Long entityId, String entityType, az.fitnest.catalog.model.entity.StoreAddress address, String fieldName, String userLanguage) {
        if (address == null) return null;
        String localized = translationService.getTranslatedValue(entityType, entityId.toString(), fieldName, userLanguage);
        if (localized == null || localized.isEmpty()) {
            try {
                java.lang.reflect.Field f = address.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object v = f.get(address);
                if (v != null) return v.toString();
            } catch (Exception ignored) {
            }
            return null;
        }
        return localized;
    }
}
