package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.GymMainPageResponse;
import az.fitnest.catalog.dto.response.GymPlanItemResponse;
import az.fitnest.catalog.dto.response.GymSubscriptionCountResponse;
import az.fitnest.catalog.dto.response.LandingCategoryItem;
import az.fitnest.catalog.dto.response.LandingGymCardResponse;
import az.fitnest.catalog.dto.response.LandingGymDetailResponse;
import az.fitnest.catalog.dto.response.LandingGymFiltersResponse;
import az.fitnest.catalog.dto.response.LandingStatsResponse;
import az.fitnest.catalog.dto.response.LandingStoreCardResponse;
import az.fitnest.catalog.dto.response.LandingStoreDetailResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.GymMapper;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymDescription;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.Room;
import az.fitnest.catalog.model.entity.RoomImage;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.model.entity.StoreDiscount;
import az.fitnest.catalog.model.entity.StoreWorkHours;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.model.enums.StoreStatus;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.GymReadService;
import az.fitnest.catalog.service.LandingPublicService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.util.AzerbaijanLocations;
import az.fitnest.catalog.util.PublicLandingMedia;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.order.grpc.SubscriptionPackageInfo;
import az.fitnest.order.grpc.SubscriptionPackageOption;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandingPublicServiceImpl implements LandingPublicService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE = 40;
    private static final int HOME_PAGE_SIZE = 3;
    private static final int MAX_GALLERY_IMAGES = 16;
    private static final int MAX_AMENITIES = 24;
    private static final int MAX_DESCRIPTION_CHARS = 2000;
    private static final DateTimeFormatter STORE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final GymReadService gymReadService;
    private final GymRepository gymRepository;
    private final GymImageRepository gymImageRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final TranslationService translationService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "landing-stats", key = "T(az.fitnest.catalog.util.UserContext).getUserLanguage()")
    public LandingStatsResponse getStats() {
        long gymCount = gymRepository.countByStatus(GymStatus.ACTIVE);
        long platinumGymCount = gymReadService.getGymCountBySubscription().stream()
                .filter(item -> isPlatinum(item.subscriptionName()))
                .mapToLong(GymSubscriptionCountResponse::count)
                .max()
                .orElse(0L);

        int packageCount = 0;
        int monthlyVisitLimit = 0;
        try {
            List<SubscriptionPackageInfo> packages = orderServiceGrpcClient.getGymPlans();
            List<SubscriptionPackageInfo> activePackages = packages.stream()
                    .filter(SubscriptionPackageInfo::getIsActive)
                    .toList();
            if (activePackages.isEmpty()) {
                activePackages = packages;
            }
            packageCount = activePackages.size();
            monthlyVisitLimit = resolveMonthlyVisitLimit(activePackages);
        } catch (Exception ignored) {
        }

        return LandingStatsResponse.builder()
                .gymCount(gymCount)
                .platinumGymCount(platinumGymCount)
                .monthlyVisitLimit(monthlyVisitLimit)
                .packageCount(packageCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "landing-home-gyms", key = "T(az.fitnest.catalog.util.UserContext).getUserLanguage()")
    public PaginatedResponse<LandingGymCardResponse> getHomeGyms() {
        return loadGymCards(1, HOME_PAGE_SIZE);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "landing-gyms",
            key = "{#page, #pageSize, #q, #city, #category, #membership, T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public PaginatedResponse<LandingGymCardResponse> getGyms(
            int page, int pageSize, String q, String city, String category, String membership) {
        return loadGymCards(page, pageSize, q, city, category, membership);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "landing-gym-filters", key = "T(az.fitnest.catalog.util.UserContext).getUserLanguage()")
    public LandingGymFiltersResponse getGymFilters() {
        String language = UserContext.getUserLanguage();
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (Category category : gymRepository.findDistinctMainCategoriesByStatus(GymStatus.ACTIVE)) {
            addLocalizedCategory(categories, category, language);
        }
        for (Category category : gymRepository.findDistinctSubCategoriesByStatus(GymStatus.ACTIVE)) {
            addLocalizedCategory(categories, category, language);
        }
        List<String> cities = AzerbaijanLocations.CITIES;
        return LandingGymFiltersResponse.builder()
                .cities(cities)
                .categories(categories.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList())
                .memberships(List.of("bronze", "silver", "gold", "platinum"))
                .build();
    }

    private PaginatedResponse<LandingGymCardResponse> loadGymCards(int page, int pageSize) {
        return loadGymCards(page, pageSize, null, null, null, null);
    }

    private PaginatedResponse<LandingGymCardResponse> loadGymCards(
            int page, int pageSize, String q, String city, String category, String membership) {
        int safePage = Math.min(Math.max(page, 1), MAX_PAGE);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        PaginatedResponse<GymMainPageResponse> gyms = gymReadService.getGyms(
                null, blankToNull(q), "ALL", null, null, safePage, safePageSize, null, null, "desc");

        List<Long> ids = gyms.items().stream()
                .map(item -> parseGymId(item.gymId()))
                .filter(Objects::nonNull)
                .toList();
        String language = UserContext.getUserLanguage();
        Map<Long, Gym> gymById = ids.isEmpty()
                ? Map.of()
                : gymRepository.findActiveWithCategoriesByIdIn(ids, GymStatus.ACTIVE).stream()
                .collect(Collectors.toMap(Gym::getId, gym -> gym, (left, right) -> left));

        String cityFilter = blankToNull(city);
        String categoryFilter = blankToNull(category);
        String membershipFilter = normalizeMembership(membership);

        List<LandingGymCardResponse> items = gyms.items().stream()
                .map(item -> {
                    Long gymId = parseGymId(item.gymId());
                    Gym gym = gymId == null ? null : gymById.get(gymId);
                    return gym == null ? null : toGymCard(item, gym, language);
                })
                .filter(Objects::nonNull)
                .filter(item -> cityFilter == null || AzerbaijanLocations.matches(item.city(), cityFilter))
                .filter(item -> categoryFilter == null || matchesCategory(item, categoryFilter))
                .filter(item -> membershipFilter == null || membershipFilter.equals(item.membership()))
                .toList();

        return PaginatedResponse.<LandingGymCardResponse>builder()
                .items(items)
                .total(gyms.total())
                .page(gyms.page())
                .pageSize(gyms.pageSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "landing-gym-detail",
            key = "{#gymId, T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public LandingGymDetailResponse getGym(Long gymId) {
        if (gymId == null || gymId < 1L) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        if (gym.getStatus() != GymStatus.ACTIVE) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }

        String language = UserContext.getUserLanguage();
        String location = gym.getAddress() != null
                ? publicText(AzerbaijanLocations.repairMojibake(gym.getAddress().getAddressText()), 200)
                : null;
        String city = gym.getAddress() != null
                ? publicText(AzerbaijanLocations.canonical(gym.getAddress().getCity()), 80)
                : null;
        Double latitude = publicLatitude(gym.getAddress() != null ? gym.getAddress().getLatitude() : null);
        Double longitude = publicLongitude(gym.getAddress() != null ? gym.getAddress().getLongitude() : null);

        List<LandingCategoryItem> categoryItems = resolveCategoryItems(gym, language);
        List<String> categories = categoryItems.stream().map(LandingCategoryItem::name).toList();
        String categoryName = categories.isEmpty() ? null : String.join(" & ", categories);

        List<String> workHours = resolveWorkHours(gym, language);
        List<GymImage> galleryImages = gymImageRepository.findByGymId(gymId);
        List<String> accessMemberships = resolveAccessMemberships(gym);
        String description = resolveDescription(gym, language);
        List<String> amenities = toAmenities(gym, language);
        NoteSplit notes = splitNote(description, amenities);

        return LandingGymDetailResponse.builder()
                .gymId(gym.getId().toString())
                .name(publicText(gym.getName(), 120))
                .coverImageUrl(PublicLandingMedia.toPublicUrl(gym.getCoverImageUrl()))
                .galleryImageUrls(toGalleryUrls(gym, galleryImages))
                .location(location)
                .city(city)
                .latitude(latitude)
                .longitude(longitude)
                .phone(publicText(resolvePhone(gym), 32))
                .workHours(workHours.isEmpty() ? List.of() : workHours)
                .category(publicText(categoryName, 160))
                .categories(categories)
                .categoryItems(categoryItems)
                .membership(accessMemberships.stream()
                        .max(Comparator.comparingInt(this::membershipRank))
                        .orElse("bronze"))
                .accessMemberships(accessMemberships)
                .description(notes.description())
                .amenities(notes.amenities())
                .note(notes.note())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "landing-home-stores", key = "T(az.fitnest.catalog.util.UserContext).getUserLanguage()")
    public PaginatedResponse<LandingStoreCardResponse> getHomeStores() {
        return loadStoreCards(1, HOME_PAGE_SIZE);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "landing-stores",
            key = "{#page, #pageSize, T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public PaginatedResponse<LandingStoreCardResponse> getStores(int page, int pageSize) {
        return loadStoreCards(page, pageSize);
    }

    private PaginatedResponse<LandingStoreCardResponse> loadStoreCards(int page, int pageSize) {
        int safePage = Math.min(Math.max(page, 1), MAX_PAGE);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        Page<Store> storePage = storeRepository.findByStatusIgnoreCase(
                StoreStatus.ACTIVE.name(),
                PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "createdDate")));

        String language = UserContext.getUserLanguage();
        List<LandingStoreCardResponse> items = storePage.getContent().stream()
                .map(store -> toStoreCard(store, language))
                .toList();

        return PaginatedResponse.<LandingStoreCardResponse>builder()
                .items(items)
                .total(storePage.getTotalElements())
                .page(safePage)
                .pageSize(safePageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "landing-store-detail",
            key = "{#storeId, T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public LandingStoreDetailResponse getStore(Long storeId) {
        if (storeId == null || storeId < 1L) {
            throw new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found");
        }
        Store store = storeRepository.findByIdWithAssociations(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found"));
        if (store.getStatus() == null || !StoreStatus.ACTIVE.name().equalsIgnoreCase(store.getStatus())) {
            throw new ResourceNotFoundException("STORE_NOT_FOUND", "error.store_not_found");
        }
        return toStoreDetail(store, UserContext.getUserLanguage());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "landing-media-public", key = "#fileId", unless = "#result == false")
    public boolean isPublicLandingMedia(String fileId) {
        if (!PublicLandingMedia.isSafeFileId(fileId)) {
            return false;
        }
        return gymRepository.existsActivePublicCoverFile(fileId)
                || storeRepository.existsActivePublicCoverFile(fileId)
                || categoryRepository.existsPublicIconFile(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public void streamPublicLandingMedia(String fileId, java.io.OutputStream outputStream) {
        if (!isPublicLandingMedia(fileId)) {
            throw new ResourceNotFoundException("MEDIA_NOT_FOUND", "error.file_not_found");
        }
        try {
            fileStorageService.streamFileToOutput(fileId, outputStream);
        } catch (RuntimeException ex) {
            throw new ResourceNotFoundException("MEDIA_NOT_FOUND", "error.file_not_found");
        }
    }

    private LandingGymCardResponse toGymCard(GymMainPageResponse item, Gym gym, String language) {
        List<LandingCategoryItem> categoryItems = resolveCategoryItems(gym, language);
        List<String> categories = categoryItems.stream().map(LandingCategoryItem::name).toList();
        String categoryName = categories.isEmpty()
                ? (item.category() != null ? item.category().name() : null)
                : String.join(" & ", categories);
        String city = gym.getAddress() != null
                ? publicText(AzerbaijanLocations.canonical(gym.getAddress().getCity()), 80)
                : publicText(item.city(), 80);
        String location = gym.getAddress() != null
                ? publicText(AzerbaijanLocations.repairMojibake(gym.getAddress().getAddressText()), 200)
                : publicText(item.location(), 200);
        return LandingGymCardResponse.builder()
                .gymId(item.gymId())
                .name(publicText(gym.getName(), 120))
                .coverImageUrl(PublicLandingMedia.toPublicUrl(item.coverImageUrl()))
                .location(location)
                .city(city)
                .phone(publicText(resolvePhone(gym), 32))
                .category(publicText(categoryName, 160))
                .categories(categories)
                .categoryItems(categoryItems)
                .membership(resolveMembership(item.supportedSubscriptions()))
                .build();
    }

    private boolean matchesCategory(LandingGymCardResponse item, String categoryFilter) {
        String needle = categoryFilter.toLowerCase(Locale.ROOT);
        if (item.categories() != null) {
            for (String name : item.categories()) {
                if (name != null && name.toLowerCase(Locale.ROOT).equals(needle)) {
                    return true;
                }
            }
        }
        return item.category() != null && item.category().toLowerCase(Locale.ROOT).contains(needle);
    }

    private List<String> resolveWorkHours(Gym gym, String language) {
        List<String> general = GymMapper.toWorkHoursLines(gym.getGeneralWorkHours(), language);
        if (!general.isEmpty()) {
            return general;
        }
        LinkedHashSet<String> hours = new LinkedHashSet<>();
        hours.addAll(GymMapper.toWorkHoursLines(gym.getWorkHoursWoman(), language));
        hours.addAll(GymMapper.toWorkHoursLines(gym.getWorkHoursMan(), language));
        return List.copyOf(hours);
    }

    private LandingStoreCardResponse toStoreCard(Store store, String language) {
        String localizedName = store.getName();

        String city = store.getAddress() != null
                ? AzerbaijanLocations.canonical(store.getAddress().getCity())
                : null;
        String addressText = store.getAddress() != null
                ? AzerbaijanLocations.repairMojibake(store.getAddress().getAddressText())
                : null;

        List<String> discounts = store.getDiscounts() == null
                ? List.of()
                : store.getDiscounts().stream()
                .map(StoreDiscount::getPercent)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(percent -> percent + "%")
                .toList();

        boolean isNew = store.getCreatedDate() != null
                && store.getCreatedDate().isAfter(LocalDateTime.now().minusDays(30));

        return LandingStoreCardResponse.builder()
                .storeId(store.getId())
                .name(publicText(localizedName, 120))
                .coverImageUrl(PublicLandingMedia.toPublicUrl(store.getCoverImageUrl()))
                .city(publicText(city, 80))
                .addressText(publicText(addressText, 200))
                .category(publicText(store.getCategory(), 80))
                .discounts(discounts)
                .isNew(isNew)
                .phone(publicText(store.getPhone(), 32))
                .workHoursText(toWorkHoursText(store.getWorkHours()))
                .socialUrl(publicUrl(store.getSocialLink() != null ? store.getSocialLink().getUrl() : null))
                .build();
    }

    private LandingStoreDetailResponse toStoreDetail(Store store, String language) {
        LandingStoreCardResponse card = toStoreCard(store, language);
        return LandingStoreDetailResponse.builder()
                .storeId(card.storeId())
                .name(card.name())
                .coverImageUrl(card.coverImageUrl())
                .city(card.city())
                .addressText(card.addressText())
                .category(card.category())
                .phone(card.phone())
                .email(publicText(store.getEmail(), 120))
                .workHoursText(card.workHoursText())
                .discounts(card.discounts())
                .isNew(card.isNew())
                .socialUrl(card.socialUrl())
                .build();
    }

    private String toWorkHoursText(StoreWorkHours workHours) {
        if (workHours == null || workHours.getFromTime() == null || workHours.getToTime() == null) {
            return null;
        }
        return workHours.getFromTime().format(STORE_TIME_FMT)
                + " - "
                + workHours.getToTime().format(STORE_TIME_FMT);
    }

    private String resolveMembership(List<GymPlanItemResponse> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return "bronze";
        }
        return subscriptions.stream()
                .map(GymPlanItemResponse::packageName)
                .map(this::membershipFromName)
                .max(Comparator.comparingInt(this::membershipRank))
                .orElse("bronze");
    }

    private List<String> resolveAccessMemberships(Gym gym) {
        if (gym.getSubscriptions() == null || gym.getSubscriptions().isEmpty()) {
            return List.of("bronze");
        }
        List<Long> packageIds = gym.getSubscriptions().stream()
                .map(GymSubscription::getPackageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (packageIds.isEmpty()) {
            return List.of("bronze");
        }
        try {
            List<String> tiers = orderServiceGrpcClient.getPackageNamesByIds(packageIds).stream()
                    .map(az.fitnest.order.grpc.PackageNameInfo::getName)
                    .map(this::membershipFromName)
                    .distinct()
                    .sorted(Comparator.comparingInt(this::membershipRank))
                    .toList();
            return tiers.isEmpty() ? List.of("bronze") : tiers;
        } catch (Exception ignored) {
            return List.of("bronze");
        }
    }

    private List<String> toAmenities(Gym gym, String language) {
        if (gym.getSubscriptions() == null || gym.getSubscriptions().isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (GymSubscription subscription : gym.getSubscriptions()) {
            Set<SupportedService> services = subscription.getSupportedServices();
            if (services == null) {
                continue;
            }
            for (SupportedService service : services) {
                if (service == null) {
                    continue;
                }
                String localized = translationService.getTranslatedValue(
                        "SUPPORTEDSERVICE",
                        String.valueOf(service.getId()),
                        "name",
                        language);
                String name = (localized != null && !localized.isBlank()) ? localized : service.getName();
                if (name != null && !name.isBlank()) {
                    names.add(name.trim());
                }
            }
        }
        if (gym.getRooms() != null) {
            for (Room room : gym.getRooms()) {
                if (room == null) {
                    continue;
                }
                addNamedAmenity(names, room.getName());
                if (room.getCategory() != null) {
                    String localizedRoomCategory = translationService.getTranslatedValue(
                            "CATEGORY",
                            String.valueOf(room.getCategory().getCategoryId()),
                            "name",
                            language);
                    addNamedAmenity(names, firstNonBlank(localizedRoomCategory, room.getCategory().getName()));
                }
            }
        }
        return names.stream().limit(MAX_AMENITIES).toList();
    }

    private void addNamedAmenity(LinkedHashSet<String> names, String value) {
        if (value != null && !value.isBlank()) {
            names.add(value.trim());
        }
    }

    private List<LandingCategoryItem> resolveCategoryItems(Gym gym, String language) {
        java.util.LinkedHashMap<String, LandingCategoryItem> items = new java.util.LinkedHashMap<>();
        if (gym.getMainCategories() != null) {
            gym.getMainCategories().forEach(category -> addCategoryItem(items, category, language));
        }
        if (gym.getSubCategories() != null) {
            gym.getSubCategories().forEach(category -> addCategoryItem(items, category, language));
        }
        return items.values().stream().limit(8).toList();
    }

    private void addCategoryItem(
            java.util.LinkedHashMap<String, LandingCategoryItem> items,
            Category category,
            String language
    ) {
        if (category == null) {
            return;
        }
        String localized = translationService.getTranslatedValue(
                "CATEGORY", String.valueOf(category.getCategoryId()), "name", language);
        String name = firstNonBlank(localized, category.getName());
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        items.putIfAbsent(trimmed, LandingCategoryItem.builder()
                .name(trimmed)
                .iconUrl(PublicLandingMedia.toPublicUrl(category.getIconUrl()))
                .build());
    }

    private String resolvePhone(Gym gym) {
        String topLevel = publicText(gym.getPhone(), 32);
        if (topLevel != null) {
            return topLevel;
        }
        if (gym.getDescriptions() == null) {
            return null;
        }
        for (GymDescription extra : gym.getDescriptions()) {
            if (extra != null && extra.getPhone() != null && !extra.getPhone().isBlank()) {
                return publicText(extra.getPhone(), 32);
            }
        }
        return null;
    }

    private NoteSplit splitNote(String description, List<String> amenities) {
        java.util.ArrayList<String> kept = new java.util.ArrayList<>();
        String note = null;
        if (amenities != null) {
            for (String amenity : amenities) {
                String extracted = extractPrefixedNote(amenity);
                if (extracted != null) {
                    note = firstNonBlank(note, extracted);
                } else {
                    kept.add(amenity);
                }
            }
        }
        String body = description;
        if (body != null) {
            java.util.regex.Matcher matcher = NOTE_IN_TEXT.matcher(body);
            if (matcher.find()) {
                note = firstNonBlank(note, matcher.group(3).trim());
                body = publicText(matcher.group(1).trim(), MAX_DESCRIPTION_CHARS);
            }
        }
        return new NoteSplit(body, List.copyOf(kept), publicText(note, 500));
    }

    private String extractPrefixedNote(String value) {
        if (value == null) {
            return null;
        }
        java.util.regex.Matcher matcher = NOTE_PREFIX.matcher(value.trim());
        return matcher.matches() ? matcher.group(2).trim() : null;
    }

    private String publicUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.length() > 300 ? trimmed.substring(0, 300) : trimmed;
        }
        return null;
    }

    private record NoteSplit(String description, List<String> amenities, String note) {
    }

    private static final java.util.regex.Pattern NOTE_PREFIX = java.util.regex.Pattern.compile(
            "(?i)^(qeyd|note|примечание)\\s*[:\\-–]\\s*(.+)$");
    private static final java.util.regex.Pattern NOTE_IN_TEXT = java.util.regex.Pattern.compile(
            "(?is)^(.*)(?:^|\\n)\\s*(qeyd|note|примечание)\\s*[:\\-–]\\s*(.+)$");

    private List<String> resolveCategories(Gym gym, String language) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (gym.getMainCategories() != null) {
            gym.getMainCategories().forEach(category -> addLocalizedCategory(names, category, language));
        }
        if (gym.getSubCategories() != null) {
            gym.getSubCategories().forEach(category -> addLocalizedCategory(names, category, language));
        }
        return names.stream().limit(8).toList();
    }

    private void addLocalizedCategory(LinkedHashSet<String> names, Category category, String language) {
        if (category == null) {
            return;
        }
        String localized = translationService.getTranslatedValue(
                "CATEGORY", String.valueOf(category.getCategoryId()), "name", language);
        String name = firstNonBlank(localized, category.getName());
        if (name != null && !name.isBlank()) {
            names.add(name.trim());
        }
    }

    private String resolveDescription(Gym gym, String language) {
        String localized = firstNonBlank(translationService.getTranslatedValue(
                "GYM", gym.getId().toString(), "description", language), gym.getDescription());
        if (localized != null && !localized.isBlank()) {
            return publicText(localized, MAX_DESCRIPTION_CHARS);
        }
        if (gym.getDescriptions() == null) {
            return null;
        }
        for (GymDescription extra : gym.getDescriptions()) {
            if (extra != null && extra.getDescription() != null && !extra.getDescription().isBlank()) {
                return publicText(extra.getDescription(), MAX_DESCRIPTION_CHARS);
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeMembership(String membership) {
        String value = blankToNull(membership);
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "bronze", "silver", "gold", "platinum" -> lower;
            default -> null;
        };
    }

    private List<String> toGalleryUrls(Gym gym, List<GymImage> images) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        addPublicUrl(urls, gym.getCoverImageUrl());
        if (images != null) {
            for (GymImage image : images) {
                if (image == null || isLogoImage(image)) {
                    continue;
                }
                addPublicUrl(urls, image.getUrl());
            }
        }
        if (gym.getRooms() != null) {
            for (Room room : gym.getRooms()) {
                if (room == null || room.getImages() == null) {
                    continue;
                }
                for (RoomImage roomImage : room.getImages()) {
                    if (roomImage == null) {
                        continue;
                    }
                    addPublicUrl(urls, roomImage.getPictureUrl());
                }
            }
        }
        return urls.stream().limit(MAX_GALLERY_IMAGES).toList();
    }

    private void addPublicUrl(LinkedHashSet<String> urls, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String publicUrl = PublicLandingMedia.toPublicUrl(raw.trim());
        if (publicUrl != null && !publicUrl.isBlank()) {
            urls.add(publicUrl);
        }
    }

    private boolean isLogoImage(GymImage image) {
        return matchesLogo(image.getType()) || matchesLogo(image.getImageName()) || matchesLogo(image.getTitle());
    }

    private boolean matchesLogo(String value) {
        return value != null && "logo".equalsIgnoreCase(value.trim());
    }

    private String membershipFromName(String name) {
        if (name == null) {
            return "bronze";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("platinum") || lower.contains("platin")) {
            return "platinum";
        }
        if (lower.contains("gold") || lower.contains("qızıl") || lower.contains("qizil")) {
            return "gold";
        }
        if (lower.contains("silver") || lower.contains("gümüş") || lower.contains("gumus")) {
            return "silver";
        }
        return "bronze";
    }

    private int membershipRank(String membership) {
        return switch (membership) {
            case "platinum" -> 4;
            case "gold" -> 3;
            case "silver" -> 2;
            default -> 1;
        };
    }

    private boolean isPlatinum(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("platinum") || lower.contains("platin");
    }

    private int resolveMonthlyVisitLimit(List<SubscriptionPackageInfo> packages) {
        SubscriptionPackageInfo bronze = packages.stream()
                .filter(pkg -> isBronze(pkg.getName()))
                .findFirst()
                .orElse(packages.isEmpty() ? null : packages.get(0));
        if (bronze == null) {
            return 0;
        }
        return bronze.getOptionsList().stream()
                .filter(option -> option.getDurationMonths() == 1)
                .mapToInt(SubscriptionPackageOption::getEntryLimit)
                .findFirst()
                .orElseGet(() -> bronze.getOptionsList().stream()
                        .mapToInt(SubscriptionPackageOption::getEntryLimit)
                        .findFirst()
                        .orElse(0));
    }

    private boolean isBronze(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("bronze") || lower.contains("bürünc") || lower.contains("burunc");
    }

    private Long parseGymId(String gymId) {
        if (gymId == null || gymId.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(gymId);
            return parsed < 1L ? null : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private String publicText(String value, int maxChars) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxChars ? trimmed.substring(0, maxChars) : trimmed;
    }

    private Double publicLatitude(Double latitude) {
        if (latitude == null || latitude.isNaN() || latitude.isInfinite() || latitude < -90d || latitude > 90d) {
            return null;
        }
        return latitude;
    }

    private Double publicLongitude(Double longitude) {
        if (longitude == null || longitude.isNaN() || longitude.isInfinite() || longitude < -180d || longitude > 180d) {
            return null;
        }
        return longitude;
    }
}
