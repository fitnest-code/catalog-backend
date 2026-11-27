package az.fitnest.catalog.service.category;

import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.PackagePriceItem;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.ServiceItem;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.WorkHourItem;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryServiceLink;
import az.fitnest.catalog.model.entity.GymCategoryWorkHour;
import az.fitnest.catalog.model.entity.GymDescription;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.entity.Room;
import az.fitnest.catalog.model.entity.RoomImage;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymCategoryServiceLinkRepository;
import az.fitnest.catalog.repository.GymCategoryWorkHourRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the per-category offering blocks (main + sub) for gym detail v5 and the QR category chips.
 * Main-category prices keep coming from {@code gym_package_price} (home-gym/V4 stay the source of truth);
 * subcategory prices come from {@code gym_category_package_price}. Must run inside a transaction because
 * it walks lazy gym graph members (descriptions, rooms, subscriptions).
 */
@Component
@RequiredArgsConstructor
public class GymCategoryOfferingsService {

    public static final String ROLE_MAIN = "MAIN";
    public static final String ROLE_SUB = "SUB";
    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final GymPackagePriceRepository gymPrices;
    private final GymCategoryPackagePriceRepository categoryPrices;
    private final GymCategoryWorkHourRepository categoryWorkHours;
    private final GymCategoryServiceLinkRepository categoryServices;
    private final SupportedServiceRepository supportedServices;
    private final HomeGymRuleService homeGymRuleService;
    private final TranslationService translationService;

    /** Ordered (main first, then sub) category + role pairs of the gym. */
    public static List<Map.Entry<Category, String>> offeredCategories(Gym gym) {
        List<Map.Entry<Category, String>> out = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        if (gym.getMainCategories() != null) {
            gym.getMainCategories().stream()
                    .filter(c -> c != null && c.getId() != null)
                    .sorted(Comparator.comparing(Category::getId))
                    .forEach(c -> {
                        if (seen.add(c.getId())) out.add(Map.entry(c, ROLE_MAIN));
                    });
        }
        if (gym.getSubCategories() != null) {
            gym.getSubCategories().stream()
                    .filter(c -> c != null && c.getId() != null)
                    .sorted(Comparator.comparing(Category::getId))
                    .forEach(c -> {
                        if (seen.add(c.getId())) out.add(Map.entry(c, ROLE_SUB));
                    });
        }
        return out;
    }

    public static boolean hasSubcategories(Gym gym) {
        return !CategoryEntryPriceResolver.subCategoryIds(gym).isEmpty()
                || Boolean.TRUE.equals(gym.getHasSubcategories());
    }

    /** Picker is mandatory as soon as there is a sub, or more than one category to choose from. */
    public static boolean categorySelectionRequired(Gym gym) {
        return hasSubcategories(gym) || CategoryEntryPriceResolver.offeredCategoryIds(gym).size() > 1;
    }

    public String localizedCategoryName(Category c, String lang) {
        String localized = lang == null ? null
                : translationService.getTranslatedValue("CATEGORY", c.getId().toString(), "name", lang);
        return localized != null && !localized.isEmpty() ? localized : c.getName();
    }

    public List<GymCategoryOfferingV5> buildOfferings(Gym gym, String lang) {
        List<Map.Entry<Category, String>> cats = offeredCategories(gym);
        if (cats.isEmpty()) {
            return List.of();
        }
        Long gymId = gym.getId();
        LocalDate today = LocalDate.now(BAKU);
        List<Long> categoryIds = cats.stream().map(e -> e.getKey().getId()).toList();

        // One query per table for the whole gym instead of N per category.
        Map<Long, List<GymCategoryWorkHour>> hoursByCat = categoryWorkHours
                .findByGymIdAndCategoryIdIn(gymId, categoryIds).stream()
                .collect(Collectors.groupingBy(GymCategoryWorkHour::getCategoryId));
        Map<Long, List<GymCategoryServiceLink>> linksByCat = categoryServices
                .findByGymIdAndCategoryIdIn(gymId, categoryIds).stream()
                .collect(Collectors.groupingBy(GymCategoryServiceLink::getCategoryId));
        Map<Long, SupportedService> servicesById = supportedServices
                .findAllById(linksByCat.values().stream()
                        .flatMap(List::stream)
                        .map(GymCategoryServiceLink::getSupportedServiceId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SupportedService::getId, s -> s));
        Map<Long, List<az.fitnest.catalog.model.entity.GymCategoryPackagePrice>> subPricesByCat = categoryPrices
                .findLatestByGymAndCategories(gymId, categoryIds, today).stream()
                .collect(Collectors.groupingBy(p -> p.getCategoryId()));
        List<GymPackagePrice> mainPrices = gymPrices.findLatestByGymIdIn(List.of(gymId), today).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getIncluded()))
                .toList();

        List<GymCategoryOfferingV5> result = new ArrayList<>();
        for (Map.Entry<Category, String> entry : cats) {
            Category c = entry.getKey();
            String role = entry.getValue();
            GymDescription desc = findDescription(gym, c.getId());

            List<PackagePriceItem> prices;
            if (ROLE_MAIN.equals(role)) {
                prices = mainPrices.stream()
                        .map(p -> new PackagePriceItem(p.getPackageId(), packageName(p.getPackageId()),
                                p.getSinglePrice(), p.getUnlimitedPrice()))
                        .sorted(Comparator.comparing(PackagePriceItem::packageId))
                        .toList();
            } else {
                prices = subPricesByCat.getOrDefault(c.getId(), List.of()).stream()
                        .map(p -> new PackagePriceItem(p.getPackageId(), packageName(p.getPackageId()),
                                p.getSinglePrice(), p.getUnlimitedPrice()))
                        .sorted(Comparator.comparing(PackagePriceItem::packageId))
                        .toList();
            }

            result.add(new GymCategoryOfferingV5(
                    c.getId(),
                    localizedCategoryName(c, lang),
                    role,
                    desc != null && desc.getPhone() != null ? desc.getPhone() : gym.getPhone(),
                    localizedOfferingDescription(gym, c.getId(), role, desc, lang),
                    desc != null && desc.getCoverImageUrl() != null ? desc.getCoverImageUrl() : c.getPhotoUrl(),
                    roomImageUrls(gym, c.getId()),
                    services(gym, role, linksByCat.get(c.getId()), servicesById, lang),
                    workHours(gym, hoursByCat.get(c.getId())),
                    prices));
        }
        return result;
    }

    /**
     * Prefer admin EN/RU rows for {@code GYM.description_{categoryId}} (or main {@code description});
     * fall back to the AZ columns on {@code gym_descriptions} / {@code gyms}.
     */
    private String localizedOfferingDescription(Gym gym, Long categoryId, String role,
                                                GymDescription desc, String lang) {
        String field = "description_" + categoryId;
        String localized = lang == null ? null
                : translationService.getTranslatedValue("GYM", gym.getId().toString(), field, lang);
        if (localized != null && !localized.isBlank()) {
            return localized;
        }
        if (desc != null && desc.getDescription() != null && !desc.getDescription().isBlank()) {
            return desc.getDescription();
        }
        if (!ROLE_MAIN.equals(role)) {
            return null;
        }
        String main = lang == null ? null
                : translationService.getTranslatedValue("GYM", gym.getId().toString(), "description", lang);
        if (main != null && !main.isBlank()) {
            return main;
        }
        return gym.getDescription();
    }

    private String packageName(Long packageId) {
        try {
            return homeGymRuleService.packageContext(packageId).name();
        } catch (Exception e) {
            // Package names come from order-backend over gRPC; never fail gym detail because of it.
            return null;
        }
    }

    private static GymDescription findDescription(Gym gym, Long categoryId) {
        if (gym.getDescriptions() == null) return null;
        for (GymDescription d : gym.getDescriptions()) {
            if (d.getCategory() != null && categoryId.equals(d.getCategory().getId())) return d;
        }
        return null;
    }

    private static List<String> roomImageUrls(Gym gym, Long categoryId) {
        if (gym.getRooms() == null) return List.of();
        List<String> urls = new ArrayList<>();
        for (Room room : gym.getRooms()) {
            if (room.getCategory() == null || !categoryId.equals(room.getCategory().getId())) continue;
            if (room.getImages() == null) continue;
            for (RoomImage img : room.getImages()) {
                if (img.getPictureUrl() != null) urls.add(img.getPictureUrl());
            }
        }
        return urls;
    }

    private List<ServiceItem> services(Gym gym, String role, List<GymCategoryServiceLink> links,
                                       Map<Long, SupportedService> servicesById, String lang) {
        if (links != null && !links.isEmpty()) {
            return links.stream()
                    .map(l -> servicesById.get(l.getSupportedServiceId()))
                    .filter(s -> s != null)
                    .map(s -> new ServiceItem(s.getId(), localizedServiceName(s, lang), s.getIconUrl()))
                    .toList();
        }
        if (!ROLE_MAIN.equals(role) || gym.getSubscriptions() == null) {
            return List.of();
        }
        // Legacy main category: services were only ever stored per package (gym_subscription_services).
        Map<Long, ServiceItem> union = new LinkedHashMap<>();
        for (GymSubscription sub : gym.getSubscriptions()) {
            if (sub.getSupportedServices() == null) continue;
            for (SupportedService s : sub.getSupportedServices()) {
                union.putIfAbsent(s.getId(),
                        new ServiceItem(s.getId(), localizedServiceName(s, lang), s.getIconUrl()));
            }
        }
        return new ArrayList<>(union.values());
    }

    private String localizedServiceName(SupportedService s, String lang) {
        String localized = lang == null ? null
                : translationService.getTranslatedValue("SUPPORTEDSERVICE", s.getId().toString(), "name", lang);
        return localized != null && !localized.isBlank() ? localized : s.getName();
    }

    private static List<WorkHourItem> workHours(Gym gym, List<GymCategoryWorkHour> rows) {
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .sorted(Comparator.comparing(r -> r.getPeriod().ordinal()))
                    .map(r -> new WorkHourItem(r.getPeriod().name(), time(r.getFromTime()), time(r.getToTime())))
                    .toList();
        }
        // No category-specific hours configured: expose the gym's general hours (see V102 migration note).
        if (gym.getGeneralWorkHours() == null) return List.of();
        return gym.getGeneralWorkHours().stream()
                .filter(h -> h.getPeriod() != null)
                .sorted(Comparator.comparing((GymWorkHour h) -> h.getPeriod().ordinal()))
                .map(h -> new WorkHourItem(h.getPeriod().name(), time(h.getFromTime()), time(h.getToTime())))
                .toList();
    }

    private static String time(java.time.LocalTime t) {
        return t == null ? null : t.toString();
    }
}
