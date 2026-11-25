package az.fitnest.catalog.service.category;

import az.fitnest.catalog.dto.v5.GymCategoryAnalyticsResponseV2;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.ServiceItem;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.WorkHourItem;
import az.fitnest.catalog.dto.v5.GymCategoryPriceRequestV5;
import az.fitnest.catalog.dto.v5.GymCategoryPriceResponseV5;
import az.fitnest.catalog.dto.v5.GymCategoryServicesRequestV5;
import az.fitnest.catalog.dto.v5.GymCategoryWorkHoursRequestV5;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ForbiddenException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import az.fitnest.catalog.model.entity.GymCategoryServiceLink;
import az.fitnest.catalog.model.entity.GymCategoryWorkHour;
import az.fitnest.catalog.model.entity.GymEntranceHistory;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymAdminRepository;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymCategoryServiceLinkRepository;
import az.fitnest.catalog.repository.GymCategoryWorkHourRepository;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import az.fitnest.catalog.util.UserContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin CRUD for per-category offerings (prices, work hours, services) plus category analytics.
 *
 * <p>Prices are versioned by month start exactly like {@code HomeGymAdminService.upsertPrice}: an ordinary
 * change opens a new row for next month so historical visits keep their snapshot. Deliberate difference:
 * the very first price of a (category, package) takes effect from the current month, otherwise a freshly
 * created subcategory would answer CATEGORY_PRICE_NOT_CONFIGURED at QR until the 1st of next month.
 *
 * <p>Main-category prices stay owned by the home-gym endpoints ({@code gym_package_price}); here they are
 * readable but not writable so that table's unique key / UX is never touched.
 */
@Service
@RequiredArgsConstructor
public class GymCategoryOfferingsAdminService {

    public static final String LEGACY_KEY = "legacy";

    private final GymRepository gymRepository;
    private final GymAdminRepository gymAdminRepository;
    private final CategoryRepository categoryRepository;
    private final GymPackagePriceRepository gymPrices;
    private final GymCategoryPackagePriceRepository categoryPrices;
    private final GymCategoryWorkHourRepository workHours;
    private final GymCategoryServiceLinkRepository serviceLinks;
    private final SupportedServiceRepository supportedServices;
    private final GymEntranceHistoryRepository history;
    private final HomeGymRuleService ruleService;

    // ------------------------------------------------------------------ prices

    @Transactional(readOnly = true)
    public List<GymCategoryPriceResponseV5> getPrices(Long gymId, Long categoryId) {
        Gym gym = requireGym(gymId);
        requireCategory(gym, categoryId);
        if (isMain(gym, categoryId)) {
            return gymPrices.findByGymIdOrderByEffectiveFromDescPackageIdAsc(gymId).stream()
                    .map(p -> toResponse(categoryId, p.getPackageId(), p.getSinglePrice(), p.getUnlimitedPrice(),
                            p.getEffectiveFrom()))
                    .toList();
        }
        return categoryPrices.findByGymIdAndCategoryIdOrderByPackageIdAscEffectiveFromDesc(gymId, categoryId).stream()
                .map(p -> toResponse(categoryId, p.getPackageId(), p.getSinglePrice(), p.getUnlimitedPrice(),
                        p.getEffectiveFrom()))
                .toList();
    }

    @Transactional
    public GymCategoryPriceResponseV5 upsertPrice(Long gymId, Long categoryId, GymCategoryPriceRequestV5 request) {
        if (request == null || request.packageId() == null) {
            throw new BadRequestException("CATEGORY_PRICE_INVALID_REQUEST", "packageId is required");
        }
        Gym gym = requireGym(gymId);
        requireCategory(gym, categoryId);
        if (isMain(gym, categoryId)) {
            throw new BadRequestException("MAIN_CATEGORY_PRICE_READ_ONLY",
                    "Main category prices are managed via /api/v1/admin/gyms/{gymId}/home-gym/prices");
        }
        if (!offeredPackageIds(gym).contains(request.packageId())) {
            throw new BadRequestException("HOME_GYM_PACKAGE_NOT_IN_GYM", "Package is not offered by this gym");
        }
        requireNonNegative(request.singlePrice());
        if (request.unlimitedPrice() != null) {
            requireNonNegative(request.unlimitedPrice());
        }

        boolean firstVersion = categoryPrices
                .findByGymIdAndCategoryIdOrderByPackageIdAscEffectiveFromDesc(gymId, categoryId).stream()
                .noneMatch(p -> request.packageId().equals(p.getPackageId()));
        LocalDate currentMonth = currentMonthStart();
        LocalDate effectiveFrom;
        if (firstVersion) {
            LocalDate requested = request.effectiveFrom() == null ? null : request.effectiveFrom().withDayOfMonth(1);
            effectiveFrom = requested != null && requested.isAfter(currentMonth) ? requested : currentMonth;
        } else {
            effectiveFrom = effectiveMonth(request.effectiveFrom());
        }

        LocalDate finalEffectiveFrom = effectiveFrom;
        GymCategoryPackagePrice row = categoryPrices
                .findByGymIdAndCategoryIdOrderByPackageIdAscEffectiveFromDesc(gymId, categoryId).stream()
                .filter(p -> request.packageId().equals(p.getPackageId())
                        && finalEffectiveFrom.equals(p.getEffectiveFrom()))
                .findFirst()
                .orElseGet(GymCategoryPackagePrice::new);
        row.setGym(gym);
        row.setCategoryId(categoryId);
        row.setPackageId(request.packageId());
        row.setSinglePrice(request.singlePrice());
        row.setUnlimitedPrice(request.unlimitedPrice());
        row.setEffectiveFrom(effectiveFrom);
        GymCategoryPackagePrice saved = categoryPrices.save(row);
        return toResponse(categoryId, saved.getPackageId(), saved.getSinglePrice(), saved.getUnlimitedPrice(),
                saved.getEffectiveFrom());
    }

    // -------------------------------------------------------------- work hours

    @Transactional(readOnly = true)
    public List<WorkHourItem> getWorkHours(Long gymId, Long categoryId) {
        Gym gym = requireGym(gymId);
        requireCategory(gym, categoryId);
        return workHours.findByGymIdAndCategoryIdIn(gymId, List.of(categoryId)).stream()
                .sorted(Comparator.comparing(r -> r.getPeriod().ordinal()))
                .map(r -> new WorkHourItem(r.getPeriod().name(),
                        r.getFromTime() == null ? null : r.getFromTime().toString(),
                        r.getToTime() == null ? null : r.getToTime().toString()))
                .toList();
    }

    @Transactional
    public List<WorkHourItem> replaceWorkHours(Long gymId, Long categoryId, GymCategoryWorkHoursRequestV5 request) {
        Gym gym = requireGym(gymId);
        requireCategory(gym, categoryId);
        List<WorkHourItem> items = request == null || request.workHours() == null ? List.of() : request.workHours();

        // Validate everything before deleting so a bad payload cannot wipe the existing hours.
        List<GymCategoryWorkHour> rows = new ArrayList<>();
        for (WorkHourItem item : items) {
            if (item == null || item.period() == null) {
                throw new BadRequestException("CATEGORY_WORK_HOURS_INVALID", "period is required");
            }
            GymWorkHourPeriod period;
            try {
                period = GymWorkHourPeriod.valueOf(item.period().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("CATEGORY_WORK_HOURS_INVALID", "Unknown period: " + item.period());
            }
            rows.add(GymCategoryWorkHour.builder()
                    .gymId(gymId)
                    .categoryId(categoryId)
                    .period(period)
                    .fromTime(parseTime(item.from()))
                    .toTime(parseTime(item.to()))
                    .build());
        }
        workHours.deleteByGymIdAndCategoryId(gymId, categoryId);
        // Bulk derived delete is flushed lazily; flush so re-inserting identical rows cannot interleave.
        workHours.flush();
        workHours.saveAll(rows);
        return getWorkHours(gymId, categoryId);
    }

    // ----------------------------------------------------------------- services

    @Transactional(readOnly = true)
    public List<ServiceItem> getServices(Long gymId, Long categoryId) {
        Gym gym = requireGym(gymId);
        requireCategory(gym, categoryId);
        Set<Long> ids = serviceLinks.findByGymIdAndCategoryIdIn(gymId, List.of(categoryId)).stream()
                .map(GymCategoryServiceLink::getSupportedServiceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return supportedServices.findAllById(ids).stream()
                .sorted(Comparator.comparing(SupportedService::getId))
                .map(s -> new ServiceItem(s.getId(), s.getName(), s.getIconUrl()))
                .toList();
    }

    @Transactional
    public List<ServiceItem> replaceServices(Long gymId, Long categoryId, GymCategoryServicesRequestV5 request) {
        Gym gym = requireGym(gymId);
        requireCategory(gym, categoryId);
        Set<Long> wanted = request == null || request.serviceIds() == null
                ? Set.of()
                : new LinkedHashSet<>(request.serviceIds());
        List<SupportedService> found = supportedServices.findAllById(wanted);
        if (found.size() != wanted.size()) {
            throw new BadRequestException("CATEGORY_SERVICE_NOT_FOUND", "Unknown supported service id");
        }
        for (SupportedService s : found) {
            // Services are either global (gymId null) or custom to this gym; never another gym's.
            if (s.getGymId() != null && !s.getGymId().equals(gymId)) {
                throw new BadRequestException("CATEGORY_SERVICE_NOT_FOUND",
                        "Service " + s.getId() + " belongs to another gym");
            }
        }
        serviceLinks.deleteByGymIdAndCategoryId(gymId, categoryId);
        serviceLinks.flush();
        serviceLinks.saveAll(wanted.stream()
                .map(id -> GymCategoryServiceLink.builder()
                        .gymId(gymId)
                        .categoryId(categoryId)
                        .supportedServiceId(id)
                        .build())
                .toList());
        return getServices(gymId, categoryId);
    }

    // ---------------------------------------------------------------- analytics

    @Transactional(readOnly = true)
    public GymCategoryAnalyticsResponseV2 analyticsByCategory(Long gymId, LocalDateTime start, LocalDateTime end) {
        requireGym(gymId);
        List<GymEntranceHistory> rows = (start != null && end != null)
                ? history.findByGymIdAndScanDateBetweenOrderByScanDateDesc(gymId, start, end)
                : history.findByGymIdOrderByScanDateDesc(gymId);

        // Same success/profit rule as GymEntranceServiceImpl.getGymAnalytics.
        // [0]=successful, [1]=failed. category_id NULL rows (pre-picker scans) go to the "legacy" bucket.
        Map<Long, long[]> counts = new LinkedHashMap<>();
        long[] legacy = new long[2];
        Map<Long, Double> revenueByCat = new LinkedHashMap<>();
        double legacyRevenue = 0.0;
        long totalOk = 0;
        long totalFail = 0;
        double totalRevenue = 0.0;
        for (GymEntranceHistory h : rows) {
            boolean ok = "ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus());
            double amount = ok && h.getAmount() != null ? h.getAmount() : 0.0;
            long[] bucket = h.getCategoryId() == null
                    ? legacy
                    : counts.computeIfAbsent(h.getCategoryId(), k -> new long[2]);
            bucket[ok ? 0 : 1]++;
            if (h.getCategoryId() == null) {
                legacyRevenue += amount;
            } else {
                revenueByCat.merge(h.getCategoryId(), amount, Double::sum);
            }
            if (ok) totalOk++; else totalFail++;
            totalRevenue += amount;
        }

        Map<Long, String> names = categoryRepository.findAllById(counts.keySet()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        List<GymCategoryAnalyticsResponseV2.Row> out = new ArrayList<>();
        for (Map.Entry<Long, long[]> e : counts.entrySet()) {
            out.add(new GymCategoryAnalyticsResponseV2.Row(String.valueOf(e.getKey()), e.getKey(),
                    names.get(e.getKey()), e.getValue()[0], e.getValue()[1],
                    revenueByCat.getOrDefault(e.getKey(), 0.0)));
        }
        if (legacy[0] + legacy[1] > 0) {
            out.add(new GymCategoryAnalyticsResponseV2.Row(LEGACY_KEY, null, null, legacy[0], legacy[1],
                    legacyRevenue));
        }
        return new GymCategoryAnalyticsResponseV2(gymId, totalOk, totalFail, totalRevenue, out);
    }

    // ------------------------------------------------------------------ helpers

    private Gym requireGym(Long gymId) {
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        ensureGymAccess(gymId);
        return gym;
    }

    // Same access rule as HomeGymAdminService: platform ADMIN, or an admin explicitly linked to the gym.
    private void ensureGymAccess(Long gymId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || !gymAdminRepository.existsByGymIdAndUserId(gymId, userId)) {
            throw new ForbiddenException("error.forbidden", "FORBIDDEN");
        }
    }

    private void requireCategory(Gym gym, Long categoryId) {
        if (categoryId == null || !CategoryEntryPriceResolver.offeredCategoryIds(gym).contains(categoryId)) {
            throw new ResourceNotFoundException("CATEGORY_NOT_ON_GYM", "error.category_not_found");
        }
    }

    private static boolean isMain(Gym gym, Long categoryId) {
        return gym.getMainCategories() != null
                && gym.getMainCategories().stream().anyMatch(c -> categoryId.equals(c.getId()));
    }

    private Set<Long> offeredPackageIds(Gym gym) {
        Set<Long> ids = new LinkedHashSet<>();
        if (gym.getSubscriptions() != null) {
            for (GymSubscription s : gym.getSubscriptions()) {
                if (s.getPackageId() != null) ids.add(s.getPackageId());
            }
        }
        return ids;
    }

    private GymCategoryPriceResponseV5 toResponse(Long categoryId, Long packageId, Double single, Double unlimited,
                                                  LocalDate effectiveFrom) {
        String name = null;
        try {
            name = ruleService.packageContext(packageId).name();
        } catch (Exception ignored) {
            // package name is cosmetic; gRPC hiccup must not fail the admin screen
        }
        return new GymCategoryPriceResponseV5(categoryId, packageId, name, single, unlimited, effectiveFrom);
    }

    private static void requireNonNegative(Double v) {
        if (v == null || v < 0) {
            throw new BadRequestException("CATEGORY_PRICE_INVALID", "Price must be a non-negative number");
        }
    }

    private static LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("CATEGORY_WORK_HOURS_INVALID", "Invalid time: " + s);
        }
    }

    private static LocalDate currentMonthStart() {
        return LocalDate.now(HomeGymRuleService.BAKU).withDayOfMonth(1);
    }

    private static LocalDate effectiveMonth(LocalDate requested) {
        LocalDate nextMonthStart = currentMonthStart().plusMonths(1);
        if (requested == null) return nextMonthStart;
        LocalDate date = requested.withDayOfMonth(1);
        return date.isBefore(nextMonthStart) ? nextMonthStart : date;
    }
}
