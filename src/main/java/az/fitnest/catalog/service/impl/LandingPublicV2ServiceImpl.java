package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.GymMainPageResponse;
import az.fitnest.catalog.dto.response.GymPlanItemResponse;
import az.fitnest.catalog.dto.response.LandingCategoryItem;
import az.fitnest.catalog.dto.response.LandingGymCardV2Response;
import az.fitnest.catalog.dto.response.LandingGymDetailResponse;
import az.fitnest.catalog.dto.response.LandingGymDetailV2Response;
import az.fitnest.catalog.dto.response.LandingGymFiltersResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.GymReadService;
import az.fitnest.catalog.service.LandingPublicService;
import az.fitnest.catalog.service.LandingPublicV2Service;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.service.landing.LandingMembershipSupport;
import az.fitnest.catalog.util.AzerbaijanLocations;
import az.fitnest.catalog.util.PublicLandingMedia;
import az.fitnest.catalog.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Landing API v2. Card listing is built here because the v1 card only carries the highest tier;
 * filters and detail reuse the (already cached) v1 service so the heavy mapping lives in one place.
 */
@Service
@RequiredArgsConstructor
public class LandingPublicV2ServiceImpl implements LandingPublicV2Service {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE = 40;
    private static final int MAX_CATEGORY_ITEMS = 8;

    private final GymReadService gymReadService;
    private final GymRepository gymRepository;
    private final TranslationService translationService;
    private final LandingPublicService landingPublicService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "landing-gyms-v2",
            key = "{#page, #pageSize, #q, #city, #rayon, #category, #membership, T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public PaginatedResponse<LandingGymCardV2Response> getGyms(
            int page, int pageSize, String q, String city, String rayon, String category, String membership) {
        int safePage = Math.min(Math.max(page, 1), MAX_PAGE);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        // gymReadService caps page_size at 100 — page through so filters see every ACTIVE gym.
        List<GymMainPageResponse> allItems = new ArrayList<>();
        int fetchPage = 1;
        long reportedTotal = Long.MAX_VALUE;
        final int chunkSize = 100;
        while (allItems.size() < reportedTotal && fetchPage <= MAX_PAGE) {
            PaginatedResponse<GymMainPageResponse> chunk = gymReadService.getGyms(
                    null, blankToNull(q), "ALL", null, null, fetchPage, chunkSize, null, null, "desc");
            reportedTotal = chunk.total();
            if (chunk.items() == null || chunk.items().isEmpty()) {
                break;
            }
            allItems.addAll(chunk.items());
            if (chunk.items().size() < chunkSize) {
                break;
            }
            fetchPage++;
        }

        List<Long> ids = allItems.stream()
                .map(item -> parseGymId(item.gymId()))
                .filter(Objects::nonNull)
                .toList();
        // Repository filters ACTIVE again so a stale read-service row can never leak a non-public gym.
        Map<Long, Gym> gymById = ids.isEmpty()
                ? Map.of()
                : gymRepository.findActiveWithCategoriesByIdIn(ids, GymStatus.ACTIVE).stream()
                .collect(Collectors.toMap(Gym::getId, gym -> gym, (left, right) -> left));

        String language = UserContext.getUserLanguage();
        String cityFilter = blankToNull(city);
        String rayonFilter = blankToNull(rayon);
        String categoryFilter = blankToNull(category);
        String membershipFilter = LandingMembershipSupport.normalizeTier(membership);

        List<LandingGymCardV2Response> filtered = allItems.stream()
                .map(item -> {
                    Long gymId = parseGymId(item.gymId());
                    Gym gym = gymId == null ? null : gymById.get(gymId);
                    return gym == null ? null : toCard(item, gym, language);
                })
                .filter(Objects::nonNull)
                .filter(card -> cityFilter == null || AzerbaijanLocations.matches(card.city(), cityFilter))
                .filter(card -> rayonFilter == null || AzerbaijanLocations.matchesRayon(card.rayon(), rayonFilter))
                .filter(card -> categoryFilter == null || matchesCategory(card, categoryFilter))
                // v2 filter = exact match on the card's min entry tier, so badge and filter agree.
                .filter(card -> membershipFilter == null || membershipFilter.equals(card.minEntryMembership()))
                .toList();

        int from = Math.min((safePage - 1) * safePageSize, filtered.size());
        int to = Math.min(from + safePageSize, filtered.size());
        return PaginatedResponse.<LandingGymCardV2Response>builder()
                .items(from >= to ? List.of() : filtered.subList(from, to))
                .total(filtered.size())
                .page(safePage)
                .pageSize(safePageSize)
                .build();
    }

    @Override
    public LandingGymFiltersResponse getGymFilters() {
        // v1 result is already Redis-cached per language and identical for v2.
        return landingPublicService.getGymFilters();
    }

    @Override
    @Cacheable(
            value = "landing-gym-detail-v2",
            key = "{#gymId, T(az.fitnest.catalog.util.UserContext).getUserLanguage()}"
    )
    public LandingGymDetailV2Response getGym(Long gymId) {
        if (gymId == null || gymId < 1L) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        // v1 enforces ACTIVE-only and 404 GYM_NOT_FOUND; v2 only drops phone and swaps the tier badge.
        LandingGymDetailResponse detail = landingPublicService.getGym(gymId);
        List<String> access = detail.accessMemberships() == null || detail.accessMemberships().isEmpty()
                ? List.of(LandingMembershipSupport.BRONZE)
                : detail.accessMemberships();
        return LandingGymDetailV2Response.builder()
                .gymId(detail.gymId())
                .name(detail.name())
                .coverImageUrl(detail.coverImageUrl())
                .galleryImageUrls(detail.galleryImageUrls())
                .location(detail.location())
                .city(detail.city())
                .rayon(detail.rayon())
                .latitude(detail.latitude())
                .longitude(detail.longitude())
                .workHours(detail.workHours())
                .category(detail.category())
                .categories(detail.categories())
                .categoryItems(detail.categoryItems())
                .minEntryMembership(LandingMembershipSupport.minEntryMembership(access))
                .accessMemberships(access.stream()
                        .sorted(java.util.Comparator.comparingInt(LandingMembershipSupport::membershipRank))
                        .toList())
                .description(detail.description())
                .amenities(detail.amenities())
                .note(detail.note())
                .build();
    }

    private LandingGymCardV2Response toCard(GymMainPageResponse item, Gym gym, String language) {
        List<LandingCategoryItem> categoryItems = resolveCategoryItems(gym, language);
        List<String> categories = categoryItems.stream().map(LandingCategoryItem::name).toList();
        String categoryName = categories.isEmpty()
                ? (item.category() != null ? item.category().name() : null)
                : String.join(" & ", categories);
        boolean hasAddress = gym.getAddress() != null;
        return LandingGymCardV2Response.builder()
                .gymId(item.gymId())
                .name(publicText(gym.getName(), 120))
                .coverImageUrl(PublicLandingMedia.toPublicUrl(item.coverImageUrl()))
                .location(hasAddress
                        ? publicText(AzerbaijanLocations.repairMojibake(gym.getAddress().getAddressText()), 200)
                        : publicText(item.location(), 200))
                .city(hasAddress
                        ? publicText(AzerbaijanLocations.canonical(gym.getAddress().getCity()), 80)
                        : publicText(item.city(), 80))
                .rayon(hasAddress
                        ? publicText(AzerbaijanLocations.canonicalRayon(gym.getAddress().getRayon()), 80)
                        : null)
                .category(publicText(categoryName, 160))
                .categories(categories)
                .categoryItems(categoryItems)
                .minEntryMembership(minEntryFromPlans(item.supportedSubscriptions()))
                .build();
    }

    private String minEntryFromPlans(List<GymPlanItemResponse> plans) {
        if (plans == null || plans.isEmpty()) {
            return LandingMembershipSupport.BRONZE;
        }
        return LandingMembershipSupport.minEntryMembership(plans.stream()
                .map(GymPlanItemResponse::packageName)
                .map(LandingMembershipSupport::membershipFromName)
                .toList());
    }

    private boolean matchesCategory(LandingGymCardV2Response card, String categoryFilter) {
        String needle = categoryFilter.toLowerCase(Locale.ROOT);
        if (card.categories() != null) {
            for (String name : card.categories()) {
                if (name != null && name.toLowerCase(Locale.ROOT).equals(needle)) {
                    return true;
                }
            }
        }
        return card.category() != null && card.category().toLowerCase(Locale.ROOT).contains(needle);
    }

    private List<LandingCategoryItem> resolveCategoryItems(Gym gym, String language) {
        Map<String, LandingCategoryItem> items = new LinkedHashMap<>();
        if (gym.getMainCategories() != null) {
            gym.getMainCategories().forEach(category -> addCategoryItem(items, category, language));
        }
        if (gym.getSubCategories() != null) {
            gym.getSubCategories().forEach(category -> addCategoryItem(items, category, language));
        }
        return items.values().stream().limit(MAX_CATEGORY_ITEMS).toList();
    }

    private void addCategoryItem(Map<String, LandingCategoryItem> items, Category category, String language) {
        if (category == null) {
            return;
        }
        String localized = translationService.getTranslatedValue(
                "CATEGORY", String.valueOf(category.getCategoryId()), "name", language);
        String name = localized != null && !localized.isBlank() ? localized : category.getName();
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        items.putIfAbsent(trimmed, LandingCategoryItem.builder()
                .name(trimmed)
                .iconUrl(PublicLandingMedia.toPublicUrl(category.getIconUrl()))
                .build());
    }

    private static Long parseGymId(String gymId) {
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

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String publicText(String value, int maxChars) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() > maxChars ? trimmed.substring(0, maxChars) : trimmed;
    }
}
