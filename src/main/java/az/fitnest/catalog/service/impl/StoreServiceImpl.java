/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.criteria.Expression
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.AddressDto;
import az.fitnest.catalog.dto.FilterResponseDto;
import az.fitnest.catalog.dto.StoreDetailResponseDto;
import az.fitnest.catalog.dto.StoreDiscountDto;
import az.fitnest.catalog.dto.StoreListItemDto;
import az.fitnest.catalog.dto.StoreListResponseDto;
import az.fitnest.catalog.dto.StoreMainPageDto;
import az.fitnest.catalog.dto.LocationDto;
import az.fitnest.catalog.dto.StoreRequest;
import az.fitnest.catalog.dto.StoreResponseDto;
import az.fitnest.catalog.dto.StoreSearchItemDto;
import az.fitnest.catalog.dto.StoreSearchResponseDto;
import az.fitnest.catalog.dto.StoreSocialDto;
import az.fitnest.catalog.dto.StoreWorkHourDto;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.SavedStore;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.model.entity.StoreAddress;
import az.fitnest.catalog.model.entity.StoreImage;
import az.fitnest.catalog.model.entity.StoreWorkHours;
import az.fitnest.catalog.repository.SavedStoreRepository;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.service.StoreService;
import jakarta.persistence.criteria.Expression;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoreServiceImpl
implements StoreService {
    private final StoreRepository storeRepository;
    private final SavedStoreRepository savedStoreRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final FileStorageService fileStorageService;

    private boolean isNew(Store store) {
        if (store == null || store.getCreatedDate() == null) {
            return false;
        }
        return store.getCreatedDate().isAfter(LocalDateTime.now().minusDays(30L));
    }

    @Override
    @Transactional(readOnly=true)
    public StoreListResponseDto getStores(Long userId, int page, int pageSize) {
        List<Long> pageStoreIds;
        PageRequest pageable = PageRequest.of((int)(page - 1), (int)pageSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        Page<Store> storePage = this.storeRepository.findAllWithAssociations((Pageable)pageable);
        List<StoreListItemDto> items = storePage.getContent().stream().map(store -> this.mapToListItem((Store)store, userId, null, null)).collect(Collectors.toList());
        if (userId != null && !(pageStoreIds = items.stream().map(i -> Long.parseLong(i.getStoreId())).collect(Collectors.toList())).isEmpty()) {
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, pageStoreIds));
            items.forEach(item -> item.setIsSaved(savedStoreIds.contains(Long.parseLong(item.getStoreId()))));
        }
        return StoreListResponseDto.builder().items(items).total(storePage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly=true)
    public StoreDetailResponseDto getStoreDetail(Long userId, Long storeId) {
        Store store = this.storeRepository.findByIdWithAssociations(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
        boolean isSaved = false;
        if (userId != null) {
            List<Long> found = this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, List.of(store.getId()));
            isSaved = !found.isEmpty();
        }
        return StoreDetailResponseDto.builder().storeId(store.getId().toString()).name(store.getName()).description(store.getDescription()).address(store.getAddress() != null ? AddressDto.builder().addressText(store.getAddress().getAddressText()).latitude(store.getAddress().getLatitude()).longitude(store.getAddress().getLongitude()).build() : null).phone(store.getPhone()).category(store.getCategory()).status(store.getStatus()).workingHours(store.getWorkHours() != null ? store.getWorkHours().stream().map(wh -> new StoreWorkHourDto(wh.getDay(), wh.getFromTime(), wh.getToTime())).collect(Collectors.toList()) : new ArrayList<StoreWorkHourDto>()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).social(store.getSocial() != null ? StoreSocialDto.builder().instagramUrl(store.getSocial().getInstagramUrl()).facebookUrl(store.getSocial().getFacebookUrl()).websiteUrl(store.getSocial().getWebsiteUrl()).build() : null).images(store.getImages() != null ? store.getImages().stream().map(StoreImage::getUrl).collect(Collectors.toList()) : new ArrayList<String>()).isSaved(isSaved).isNew(this.isNew(store)).build();
    }

    @Override
    public FilterResponseDto getFilters() {
        return FilterResponseDto.builder().tabs(List.of(new FilterResponseDto.TabDto("all", "All"), new FilterResponseDto.TabDto("popular", "Popular"), new FilterResponseDto.TabDto("nearby", "Near you"), new FilterResponseDto.TabDto("new", "New"))).sortOptions(List.of("popular", "newest", "distance", "discount_desc", "name_asc")).defaultRadiusKm(20).build();
    }

    private StoreListItemDto mapToListItem(Store store, Long userId, Double lat, Double lng) {
        Double distance = null;
        if (lat != null && lng != null && store.getAddress() != null && store.getAddress().getLatitude() != null && store.getAddress().getLongitude() != null) {
            distance = this.calculateDistance(lat, lng, store.getAddress().getLatitude(), store.getAddress().getLongitude());
        }
        return StoreListItemDto.builder().storeId(store.getId().toString()).name(store.getName()).description(store.getDescription()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).logoUrl(store.getLogoUrl()).coverImageUrl(store.getCoverImageUrl()).isSaved(false).distanceKm(distance).badges((List<String>)(store.getBadges() != null ? new ArrayList<String>(store.getBadges()) : null)).isNew(this.isNew(store)).build();
    }

    @Override
    @Transactional
    public StoreDetailResponseDto createStore(StoreRequest request) {
        Store store = new Store();
        this.updateStoreFromRequest(store, request);
        Store saved = (Store)this.storeRepository.save(store);
        return this.getStoreDetail(null, saved.getId());
    }

    @Override
    @Transactional
    public StoreDetailResponseDto updateStore(Long storeId, StoreRequest request) {
        Store store = (Store)this.storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
        this.updateStoreFromRequest(store, request);
        Store saved = (Store)this.storeRepository.save(store);
        return this.getStoreDetail(null, saved.getId());
    }

    @Override
    @Transactional
    public void deleteStore(Long storeId) {
        Store store = (Store)this.storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
        this.savedStoreRepository.deleteByStoreId(storeId);
        if (store.getLogoUrl() != null) {
            this.fileStorageService.deleteFile(store.getLogoUrl());
        }
        if (store.getCoverImageUrl() != null) {
            this.fileStorageService.deleteFile(store.getCoverImageUrl());
        }
        if (store.getImages() != null && !store.getImages().isEmpty()) {
            List<String> imageUrls = store.getImages().stream().map(StoreImage::getUrl).toList();
            this.fileStorageService.deleteFiles(imageUrls);
        }
        this.storeRepository.delete(store);
    }

    private void updateStoreFromRequest(Store store, StoreRequest request) {
        boolean coordsChanged;
        store.setName(request.getName());
        store.setDescription(request.getDescription());
        Double reqLat = request.getAddress() != null ? request.getAddress().getLatitude() : null;
        Double reqLng = request.getAddress() != null ? request.getAddress().getLongitude() : null;
        boolean bl = coordsChanged = store.getAddress() == null || !Objects.equals(store.getAddress().getLatitude(), reqLat) || !Objects.equals(store.getAddress().getLongitude(), reqLng);
        store.setAddress(request.getAddress() != null ? new StoreAddress(coordsChanged ? this.resolveAddressText(reqLat, reqLng) : (store.getAddress() != null ? store.getAddress().getAddressText() : this.resolveAddressText(reqLat, reqLng)), reqLat, reqLng) : null);
        store.setPhone(request.getPhone());
        store.setCategory(request.getCategory());
        store.setStatus(request.getStatus());
        if (request.getWorkingHours() != null) {
            store.setWorkHours(request.getWorkingHours().stream().map(dto -> new StoreWorkHours(dto.getDay(), dto.getFrom(), dto.getTo())).collect(Collectors.toList()));
        }
        if (request.getBadges() != null) {
            store.setBadges(new ArrayList<String>(request.getBadges()));
        }
    }

    private String resolveAddressText(Double latitude, Double longitude) {
        return this.reverseGeocodingService.reverseGeocode(latitude, longitude);
    }

    private Double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double result = earthRadius * c;
        return (double)Math.round(result * 10.0) / 10.0;
    }

    @Override
    @Transactional(readOnly=true)
    public List<StoreMainPageDto> getMainPageStores() {
        PageRequest pageable = PageRequest.of((int)0, (int)100, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        Page<Store> storePage = this.storeRepository.findAllWithAssociations((Pageable)pageable);
        List<Store> stores = storePage.getContent();
        List<StoreMainPageDto> dtos = stores.stream().map(store -> StoreMainPageDto.builder().storeId(store.getId().toString()).name(store.getName()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).logoUrl(store.getLogoUrl()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).isNew(this.isNew(store)).build()).collect(Collectors.toList());
        return dtos;
    }

    @Override
    @Transactional
    public String uploadStoreImage(Long storeId, MultipartFile file) {
        Store store = (Store)this.storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
        String fsId = this.fileStorageService.saveFile(file, "/stores/" + storeId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        StoreImage image = new StoreImage();
        image.setUrl(fullUrl);
        image.setType("gallery");
        if (store.getImages() == null) {
            store.setImages(new LinkedHashSet<StoreImage>());
        }
        store.getImages().add(image);
        this.storeRepository.save(store);
        return fullUrl;
    }

    @Override
    @Transactional(readOnly=true)
    public StoreResponseDto getStoreMainPage(Long userId, String q, int page, int pageSize) {
        Page<Store> storePage;
        PageRequest pageable = PageRequest.of((int)(page - 1), (int)pageSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.toLowerCase() + "%";
            storePage = this.storeRepository.findAll((Specification & Serializable)(root, query, cb) -> cb.or((Expression)cb.like(cb.lower((Expression)root.get("name")), pattern), (Expression)cb.like(cb.lower((Expression)root.get("address").get("addressText")), pattern)), (Pageable)pageable);
        } else {
            storePage = this.storeRepository.findAllWithAssociations((Pageable)pageable);
        }
        List<StoreMainPageDto> items = storePage.getContent().stream().map(store -> {
            StoreMainPageDto dto = StoreMainPageDto.builder().storeId(store.getId().toString()).name(store.getName()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).logoUrl(store.getLogoUrl()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).isSaved(false).isNew(this.isNew(store)).build();
            return dto;
        }).collect(Collectors.toList());
        if (userId != null && !items.isEmpty()) {
            List<Long> pageStoreIds = items.stream().map(i -> Long.parseLong(i.getStoreId())).collect(Collectors.toList());
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, pageStoreIds));
            items.forEach(item -> item.setIsSaved(savedStoreIds.contains(Long.parseLong(item.getStoreId()))));
        }
        return StoreResponseDto.builder().items(items).total(storePage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly=true)
    public StoreResponseDto getClosestStores(Long userId, String q, int page, int pageSize, Double lat, Double lng) {
        List<Store> candidates;
        if (lat != null && lng != null) {
            double initialRadiusKm = 50.0;
            double[] bbox = this.boundingBox(lat, lng, initialRadiusKm);
            PageRequest bboxPage = PageRequest.of((int)0, (int)500, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
            Page<Store> bboxStores = this.storeRepository.findByAddressLatitudeBetweenAndAddressLongitudeBetween(bbox[0], bbox[1], bbox[2], bbox[3], (Pageable)bboxPage);
            candidates = bboxStores.getContent();
        } else {
            Page storePage;
            PageRequest pageable = PageRequest.of((int)(page - 1), (int)pageSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                storePage = this.storeRepository.findAll((Specification & Serializable)(root, query, cb) -> cb.or((Expression)cb.like(cb.lower((Expression)root.get("name")), pattern), (Expression)cb.like(cb.lower((Expression)root.get("address").get("addressText")), pattern)), (Pageable)pageable);
            } else {
                storePage = this.storeRepository.findAllWithAssociations((Pageable)pageable);
            }
            candidates = storePage.getContent();
        }
        Stream<Store> stream = candidates.stream();
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stream = stream.filter(s -> s.getName() != null && s.getName().toLowerCase().contains(lowerQ) || s.getAddress() != null && s.getAddress().getAddressText() != null && s.getAddress().getAddressText().toLowerCase().contains(lowerQ));
        }
        List<StoreMainPageDto> all = stream.map(store -> {
            StoreMainPageDto dto = StoreMainPageDto.builder().storeId(store.getId().toString()).name(store.getName()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).logoUrl(store.getLogoUrl()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).isSaved(false).distanceKm(null).isNew(this.isNew(store)).build();
            if (lat != null && lng != null && store.getAddress() != null && store.getAddress().getLatitude() != null && store.getAddress().getLongitude() != null) {
                dto.setDistanceKm(this.calculateDistance(lat, lng, store.getAddress().getLatitude(), store.getAddress().getLongitude()));
            }
            return dto;
        }).collect(Collectors.toList());
        if (userId != null && !all.isEmpty()) {
            List<Long> candidateIds = all.stream().map(i -> Long.parseLong(i.getStoreId())).collect(Collectors.toList());
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, candidateIds));
            all.forEach(item -> item.setIsSaved(savedStoreIds.contains(Long.parseLong(item.getStoreId()))));
        }
        if (lat != null && lng != null) {
            all.sort((a, b) -> {
                Double da = a.getDistanceKm();
                Double db = b.getDistanceKm();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return Double.compare(da, db);
            });
        }
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<StoreMainPageDto> pageItems = from >= all.size() ? new ArrayList<>() : new ArrayList<>(all.subList(from, to));
        return StoreResponseDto.builder().items(pageItems).total(all.size()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly=true)
    public List<StoreMainPageDto> getDiscountedStores(Long userId) {
        List<StoreMainPageDto> stores = this.getMainPageStores();
        stores = stores.stream().filter(s -> s.getDiscounts() != null && !s.getDiscounts().isEmpty()).collect(Collectors.toList());
        if (userId != null && !stores.isEmpty()) {
            List<Long> storeIds = stores.stream().map(s -> Long.parseLong(s.getStoreId())).collect(Collectors.toList());
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, storeIds));
            stores.forEach(store -> store.setIsSaved(savedStoreIds.contains(Long.parseLong(store.getStoreId()))));
        } else {
            stores.forEach(store -> store.setIsSaved(false));
        }
        return stores;
    }

    @Override
    @Transactional(readOnly=true)
    public List<StoreMainPageDto> getDiscountedStores(Long userId, String q) {
        List<StoreMainPageDto> stores = this.getDiscountedStores(userId);
        if (q != null && !q.isBlank()) {
            String lowerQ = q.toLowerCase();
            stores = stores.stream().filter(s -> s.getName() != null && s.getName().toLowerCase().contains(lowerQ) || s.getAddress() != null && s.getAddress().toLowerCase().contains(lowerQ)).collect(Collectors.toList());
        }
        return stores;
    }

    @Override
    @Transactional(readOnly=true)
    public StoreResponseDto getDiscountedStores(Long userId, String q, int page, int pageSize) {
        Page<Store> storePage;
        PageRequest pageable = PageRequest.of((int)Math.max(0, page - 1), (int)pageSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.toLowerCase() + "%";
            storePage = this.storeRepository.findDiscountedStoresByQuery(pattern, (Pageable)pageable);
        } else {
            storePage = this.storeRepository.findDiscountedStores((Pageable)pageable);
        }
        List<StoreMainPageDto> items = storePage.getContent().stream().map(store -> StoreMainPageDto.builder().storeId(store.getId().toString()).name(store.getName()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).logoUrl(store.getLogoUrl()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).isSaved(false).isNew(this.isNew((Store)store)).build()).collect(Collectors.toList());
        if (userId != null && !items.isEmpty()) {
            List<Long> pageStoreIds = items.stream().map(i -> Long.parseLong(i.getStoreId())).collect(Collectors.toList());
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, pageStoreIds));
            items.forEach(item -> item.setIsSaved(savedStoreIds.contains(Long.parseLong(item.getStoreId()))));
        } else {
            items.forEach(item -> item.setIsSaved(false));
        }
        return StoreResponseDto.builder().items(items).total(storePage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly=true)
    public StoreListResponseDto getAllStores(Long userId) {
        List<Long> pageStoreIds;
        PageRequest pageable = PageRequest.of((int)0, (int)1000, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        List<Store> stores = this.storeRepository.findAllWithAssociations((Pageable)pageable).getContent();
        List<StoreListItemDto> items = stores.stream().map(store -> this.mapToListItem(store, userId, null, null)).collect(Collectors.toList());
        if (userId != null && !items.isEmpty() && !(pageStoreIds = items.stream().map(i -> Long.parseLong(i.getStoreId())).collect(Collectors.toList())).isEmpty()) {
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, pageStoreIds));
            items.forEach(item -> item.setIsSaved(savedStoreIds.contains(Long.parseLong(item.getStoreId()))));
        }
        return StoreListResponseDto.builder().items(items).build();
    }

    @Override
    @Transactional(readOnly=true)
    public List<StoreMainPageDto> getSavedStores(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<SavedStore> savedStores = this.savedStoreRepository.findByUserId(userId);
        return savedStores.stream().map(ss -> {
            Store store = ss.getStore();
            return StoreMainPageDto.builder().storeId(store.getId().toString()).name(store.getName()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).logoUrl(store.getLogoUrl()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).isSaved(true).isNew(this.isNew(store)).build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly=true)
    public StoreResponseDto getNewStores(Long userId, String q, int page, int pageSize) {
        Page<Store> storePage;
        PageRequest pageable = PageRequest.of((int)Math.max(0, page - 1), (int)pageSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30L);
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.toLowerCase() + "%";
            storePage = this.storeRepository.findNewStoresByQuery(cutoff, pattern, (Pageable)pageable);
        } else {
            storePage = this.storeRepository.findNewStores(cutoff, (Pageable)pageable);
        }
        List<StoreMainPageDto> items = storePage.getContent().stream().map(store -> StoreMainPageDto.builder().storeId(store.getId().toString()).name(store.getName()).address(store.getAddress() != null ? store.getAddress().getAddressText() : null).logoUrl(store.getLogoUrl()).discounts(store.getDiscounts() != null ? store.getDiscounts().stream().map(d -> StoreDiscountDto.builder().percent(d.getPercent()).appliesTo(d.getAppliesTo()).build()).collect(Collectors.toList()) : new ArrayList<StoreDiscountDto>()).isSaved(false).isNew(true).build()).collect(Collectors.toList());
        if (userId != null && !items.isEmpty()) {
            List<Long> pageStoreIds = items.stream().map(i -> Long.parseLong(i.getStoreId())).collect(Collectors.toList());
            HashSet<Long> savedStoreIds = new HashSet<Long>(this.savedStoreRepository.findStoreIdsByUserIdAndStoreIdIn(userId, pageStoreIds));
            items.forEach(item -> item.setIsSaved(savedStoreIds.contains(Long.parseLong(item.getStoreId()))));
        } else {
            items.forEach(item -> item.setIsSaved(false));
        }
        return StoreResponseDto.builder().items(items).total(storePage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional
    public boolean toggleSave(Long userId, Long storeId) {
        if (userId == null) {
            return false;
        }
        Store store = (Store)this.storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
        Optional<SavedStore> existing = this.savedStoreRepository.findByUserIdAndStoreId(userId, storeId);
        if (existing.isPresent()) {
            this.savedStoreRepository.delete(existing.get());
            return false;
        }
        SavedStore savedStore = SavedStore.builder().userId(userId).store(store).build();
        this.savedStoreRepository.save(savedStore);
        return true;
    }

    @Override
    @Transactional(readOnly=true)
    public StoreSearchResponseDto searchStoresForQuery(Long userId, String q, int page, int pageSize) {
        Page<Store> storePage;
        PageRequest pageable = PageRequest.of((int)(page - 1), (int)pageSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdDate"}));
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.toLowerCase() + "%";
            storePage = this.storeRepository.findAll((Specification & Serializable)(root, query, cb) -> cb.or((Expression)cb.like(cb.lower((Expression)root.get("name")), pattern), (Expression)cb.like(cb.lower((Expression)root.get("address").get("addressText")), pattern)), (Pageable)pageable);
        } else {
            storePage = this.storeRepository.findAllWithAssociations((Pageable)pageable);
        }
        List<StoreSearchItemDto> items = storePage.getContent().stream().map(s -> StoreSearchItemDto.builder().storeId(((Store)s).getId().toString()).build()).collect(Collectors.toList());
        return StoreSearchResponseDto.builder().items(items).total(storePage.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    private double[] boundingBox(double lat, double lng, double radiusKm) {
        double R = 6371.0;
        double latRadians = Math.toRadians(lat);
        double radiusRatio = radiusKm / 6371.0;
        double minLat = lat - Math.toDegrees(radiusRatio);
        double maxLat = lat + Math.toDegrees(radiusRatio);
        double deltaLng = Math.toDegrees(Math.asin(Math.sin(radiusRatio) / Math.cos(latRadians)));
        double minLng = lng - deltaLng;
        double maxLng = lng + deltaLng;
        return new double[]{minLat, maxLat, minLng, maxLng};
    }

    public StoreServiceImpl(StoreRepository storeRepository, SavedStoreRepository savedStoreRepository, ReverseGeocodingService reverseGeocodingService, FileStorageService fileStorageService) {
        this.storeRepository = storeRepository;
        this.savedStoreRepository = savedStoreRepository;
        this.reverseGeocodingService = reverseGeocodingService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDto getStoreLocation(Long storeId) {
        Store store = this.storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
        StoreAddress addr = store.getAddress();
        if (addr == null) {
             return LocationDto.builder().build();
        }
        return LocationDto.builder()
             .addressText(addr.getAddressText())
             .latitude(addr.getLatitude())
             .longitude(addr.getLongitude())
             .build();
    }

    @Override
    @Transactional(readOnly=true)
    public Store getStoreEntityById(Long storeId) {
        return this.storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Store not found"));
    }

    @Override
    public void deleteFileSafely(String url) {
        try {
            this.fileStorageService.deleteFile(url);
        } catch (Exception e) {
            // Log intentionally swallowed per design strategy
        }
    }

    @Override
    public String uploadFileDirectly(Long storeId, MultipartFile file) {
        String fsId = this.fileStorageService.saveFile(file, "/stores/" + storeId);
        return "/api/v1/media/stream/" + fsId;
    }

    @Override
    @Transactional
    public void updateStoreLogoUrl(Long storeId, String logoUrl) {
        Store store = getStoreEntityById(storeId);
        store.setLogoUrl(logoUrl);
        this.storeRepository.save(store);
    }

    @Override
    @Transactional
    public void updateStoreCoverImageUrl(Long storeId, String coverImageUrl) {
        Store store = getStoreEntityById(storeId);
        store.setCoverImageUrl(coverImageUrl);
        this.storeRepository.save(store);
    }
}

